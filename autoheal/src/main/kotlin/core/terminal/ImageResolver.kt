package core.terminal

import core.model.BlockReason
import core.model.ImagePage
import core.model.TerminalContent
import core.model.TerminalSpec
import core.parse.Density
import core.parse.Dom
import core.parse.Node
import core.parse.UrlTools
import core.parse.ValueReader

/* ═══════════════════════════════════════════════════════════════════
   СТРАНИЦЫ ГЛАВЫ
   Главная сложность не в том, чтобы найти картинки — их на странице
   сотни. Сложность в том, чтобы отличить страницы главы от баннеров,
   аватарок, иконок соцсетей и превью «читайте также». Признак, который
   работает на всех сайтах: страницы главы лежат ОДНОЙ последовательной
   группой, с одного хоста, по общему шаблону имени.
   ═══════════════════════════════════════════════════════════════════ */

class ImageResolver {

    fun resolve(dom: Dom, spec: TerminalSpec?, baseUrl: String): TerminalContent {
        // 1. Явные селекторы из конфига — если их дал пикер или ремонт.
        spec?.imageArraySelectors?.forEach { sel ->
            val nodes = dom.select(sel)
            if (nodes.size >= MIN_PAGES) {
                val pages = fromNodes(nodes, baseUrl, spec)
                if (pages.size >= MIN_PAGES) return TerminalContent.Images(pages)
            }
        }

        // 2. Список страниц в JS-переменной: у большинства читалок
        //    массив ссылок лежит в inline-скрипте, а <img> создаются
        //    на лету — по DOM их просто нет.
        fromScripts(dom, baseUrl)?.let { return TerminalContent.Images(it) }

        // 3. Эвристика по DOM: самая длинная однородная группа картинок.
        val guessed = guessFromDom(dom, baseUrl, spec)
        if (guessed.size >= MIN_PAGES) return TerminalContent.Images(guessed)

        return TerminalContent.Unavailable(
            BlockReason.UNKNOWN,
            "страницы главы не найдены: ни селекторами, ни в скриптах, ни по разметке",
        )
    }

    /* ---------------- из узлов ---------------- */

    private fun fromNodes(nodes: List<Node>, baseUrl: String, spec: TerminalSpec?): List<ImagePage> =
        nodes.mapIndexedNotNull { i, n ->
            val src = readSrc(n) ?: return@mapIndexedNotNull null
            if (ValueReader.looksLikePlaceholder(src)) return@mapIndexedNotNull null
            ImagePage(
                index = i,
                url = UrlTools.absolutize(src, baseUrl),
                headers = spec?.requiredHeaders.orEmpty(),
                width = n.attr("width").toIntOrNull(),
                height = n.attr("height").toIntOrNull(),
            )
        }.let { dedupe(it) }

    /**
     * Порядок атрибутов важен: у ленивых картинок в src лежит заглушка,
     * а настоящий адрес — в data-*. ValueReader уже знает этот порядок.
     */
    private fun readSrc(n: Node): String? {
        for (a in LAZY_ATTRS) {
            val v = n.attr(a)
            if (v.isNotBlank() && !ValueReader.looksLikePlaceholder(v)) return v
        }
        return n.attr("src").takeIf { it.isNotBlank() }
    }

    /* ---------------- из скриптов ---------------- */

    /**
     * Ищем в inline-скриптах массив строк, похожих на адреса картинок.
     * Не парсим JS: берём все строковые литералы с расширением картинки
     * и группируем по хосту и каталогу. Настоящий список страниц —
     * самая большая такая группа.
     */
    internal fun fromScripts(dom: Dom, baseUrl: String): List<ImagePage>? {
        val candidates = ArrayList<String>()
        for (script in dom.inlineScripts()) {
            if (script.length > MAX_SCRIPT) continue
            if (IMG_HINT.none { script.contains(it, ignoreCase = true) }) continue
            for (m in STRING_LITERAL.findAll(script)) {
                val raw = m.groupValues[1]
                if (raw.length !in 8..2048) continue
                if (!IMAGE_EXT.containsMatchIn(raw)) continue
                candidates += unescape(raw)
            }
        }
        if (candidates.size < MIN_PAGES) return null

        val group = largestUniformGroup(candidates, baseUrl) ?: return null
        if (group.size < MIN_PAGES) return null

        return group.mapIndexed { i, url ->
            ImagePage(index = i, url = UrlTools.absolutize(url, baseUrl))
        }
    }

    /**
     * Самая большая группа адресов с общим хостом и общим каталогом.
     * Именно это отсекает баннеры: они лежат в /banners/, /ads/,
     * на другом хосте — и в группу страниц не попадают.
     */
    private fun largestUniformGroup(urls: List<String>, baseUrl: String): List<String>? {
        val seen = LinkedHashSet<String>()
        val ordered = urls.filter { seen.add(it) }
        if (ordered.isEmpty()) return null

        val groups = ordered.groupBy { url ->
            val abs = UrlTools.absolutize(url, baseUrl)
            val host = runCatching { java.net.URI(abs).host }.getOrNull().orEmpty()
            val dir = abs.substringBeforeLast('/', "")
            host to dir
        }
        val best = groups.maxByOrNull { it.value.size } ?: return null
        return best.value.sortedWith(NaturalOrder)
    }

    /* ---------------- эвристика по DOM ---------------- */

    /**
     * Контейнер страниц — тот, у которого больше всего картинок
     * подходящего размера, лежащих подряд. Плотность текста при этом
     * должна быть низкой: если внутри много текста, это статья
     * с иллюстрациями, а не глава.
     */
    internal fun guessFromDom(dom: Dom, baseUrl: String, spec: TerminalSpec?): List<ImagePage> {
        var bestNodes: List<Node> = emptyList()
        var bestScore = 0

        for (container in dom.walk(WALK_LIMIT)) {
            val imgs = container.select("img")
            if (imgs.size < MIN_PAGES) continue
            if (imgs.size > MAX_PAGES) continue

            val usable = imgs.filter { img ->
                val src = readSrc(img) ?: return@filter false
                !ValueReader.looksLikePlaceholder(src) && !looksDecorative(img, src)
            }
            if (usable.size < MIN_PAGES) continue

            // Текстовая плотность: у контейнера главы текста почти нет.
            val metrics = Density.of(container)
            if (metrics.textLength > TEXT_NOISE && metrics.textPerElement > 25) continue

            // Чем глубже контейнер при том же числе картинок, тем он точнее:
            // <body> тоже содержит все страницы, но вместе со всем остальным.
            val score = usable.size * 100 + container.depth
            if (score > bestScore) { bestScore = score; bestNodes = usable }
        }

        return fromNodes(bestNodes, baseUrl, spec)
    }

    /** Иконки, аватарки, кнопки: маленькие или с говорящим путём. */
    private fun looksDecorative(img: Node, src: String): Boolean {
        val w = img.attr("width").toIntOrNull()
        val h = img.attr("height").toIntOrNull()
        if (w != null && w in 1..MIN_PAGE_PX) return true
        if (h != null && h in 1..MIN_PAGE_PX) return true
        val low = src.lowercase()
        return DECORATIVE_HINTS.any { low.contains(it) }
    }

    /* ---------------- общее ---------------- */

    /** Один и тот же адрес дважды — почти всегда дубль превью и страницы. */
    private fun dedupe(pages: List<ImagePage>): List<ImagePage> {
        val seen = HashSet<String>()
        return pages.filter { seen.add(it.url) }.mapIndexed { i, p -> p.copy(index = i) }
    }

    private fun unescape(s: String): String =
        s.replace("\\/", "/").replace("\\\"", "\"").replace("\\u002F", "/", ignoreCase = true)

    /**
     * Естественный порядок: page2.jpg должна идти перед page10.jpg.
     * Лексикографический порядок здесь даёт перемешанную главу — это
     * самая заметная для читателя ошибка из возможных.
     */
    internal object NaturalOrder : Comparator<String> {
        private val CHUNK = Regex("""\d+|\D+""")
        override fun compare(a: String, b: String): Int {
            val ca = CHUNK.findAll(a).map { it.value }.toList()
            val cb = CHUNK.findAll(b).map { it.value }.toList()
            for (i in 0 until minOf(ca.size, cb.size)) {
                val x = ca[i]; val y = cb[i]
                val nx = x.toLongOrNull(); val ny = y.toLongOrNull()
                val cmp = if (nx != null && ny != null) nx.compareTo(ny) else x.compareTo(y)
                if (cmp != 0) return cmp
            }
            return ca.size.compareTo(cb.size)
        }
    }

    private companion object {
        const val MIN_PAGES = 3
        const val MAX_PAGES = 600
        const val MIN_PAGE_PX = 120
        const val WALK_LIMIT = 2500
        const val MAX_SCRIPT = 400_000
        const val TEXT_NOISE = 1500

        val LAZY_ATTRS = listOf("data-src", "data-original", "data-lazy-src", "data-url", "data-page")

        val IMAGE_EXT = Regex("""\.(jpe?g|png|webp|avif|gif)(\?|$|["'\\])""", RegexOption.IGNORE_CASE)
        val STRING_LITERAL = Regex(""""((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)'""")
            .let { Regex("""["']((?:[^"'\\]|\\.){8,2048}?)["']""") }

        val IMG_HINT = listOf("images", "pages", "chapter", "pict", "img", "list")

        val DECORATIVE_HINTS = listOf(
            "/icon", "/avatar", "/logo", "/banner", "/ads/", "/ad/", "sprite",
            "emoji", "smile", "button", "/social", "favicon", "counter",
        )
    }
}
