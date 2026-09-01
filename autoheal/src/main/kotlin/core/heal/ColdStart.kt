package core.heal

import core.model.FieldSpec
import core.model.ListingSpec
import core.model.SourceConfig
import core.model.UnitsSpec
import core.model.ValueTransform
import core.parse.Dom
import core.parse.Node

/* ═══════════════════════════════════════════════════════════════════
   ХОЛОДНЫЙ СТАРТ
   Хилер чинит СЛОМАВШИЙСЯ селектор: он ищет узел по отпечатку или по
   эталонному значению, а и то и другое появляется только после
   удачного разбора. Для сайта, который видят впервые, у конфига нет
   ни listing, ни units — экстрактор возвращает пустой список, не
   заглянув в документ ни разу, а пользователю это показывалось как
   «страница собирается скриптами».

   Здесь конфиг СОЧИНЯЕТСЯ по самому документу: находим повторяющийся
   блок карточек, внутри него — ссылку и заголовок, и собираем спеки.
   Всё поверх чистых функций над Dom, поэтому проверяется JVM-тестами
   на сохранённой разметке, без устройства.
   ═══════════════════════════════════════════════════════════════════ */

object ColdStart {

    /** Минимум однотипных карточек, ниже которого это не список. */
    private const val MIN_CARDS = 4

    /**
     * Разделы-рубрикаторы. Судим по ПОСЛЕДНЕМУ сегменту пути, а не по
     * вхождению в адрес: `/genres/action/` — жанр, а
     * `/type/tv/8376-dark-moon.html` — тайтл, хотя оба начинаются с
     * рубрики. Первая версия проверки искала слово где угодно в адресе
     * и вместе с меню выбрасывала весь каталог.
     */
    private val SECTION_WORDS = setOf(
        "genre", "genres", "year", "years", "tag", "tags", "type", "types",
        "category", "categories", "rss", "login", "register", "search", "page",
        "ongoing", "index.php",
    )

    private val NON_HTTP = Regex("""^(javascript:|#|mailto:|tel:)""", RegexOption.IGNORE_CASE)

    /**
     * Достроить конфиг до пригодного к разбору, если нужных спек нет.
     *
     * Возвращает null, когда сочинять нечего (спеки уже есть или в
     * документе нет повторяющегося блока) — вызывающий тогда идёт
     * прежним путём и ничего не теряет.
     */
    fun infer(dom: Dom, config: SourceConfig): SourceConfig? {
        val needListing = config.listing == null
        val needUnits = config.units == null
        if (!needListing && !needUnits) return null

        val raw = repeatingGroups(dom).firstOrNull() ?: return null
        /*
         * Группа однотипных детей — ещё не список карточек.
         *
         * Между карточками каталога вперемешку стоят рекламные врезки
         * того же тега: `article.baner-m` соседствует с `article.post`.
         * Первой в группе оказывалась именно врезка, поля
         * синтезировались по ней, а селектор `section article` ловил и
         * то и другое — разбор возвращал один «тайтл» с заголовком
         * баннера.
         *
         * Поэтому группа сужается до самого многочисленного набора
         * классов, и уже он даёт и образец, и селектор.
         */
        val group = dominantSubgroup(raw)
        val sample = group.firstOrNull { contentHrefOf(it) != null } ?: return null
        val itemSelector = selectorFor(sample, dom, group.size) ?: return null

        val titleSpec = titleFieldOf(sample)
        val urlSpec = urlFieldOf(sample)
        val coverSpec = coverFieldOf(sample)

        var out = config
        if (needListing) {
            out = out.copy(
                listing = ListingSpec(
                    itemSelector = itemSelector,
                    fields = buildMap {
                        titleSpec?.let { put("title", it) }
                        urlSpec?.let { put("url", it) }
                        coverSpec?.let { put("cover", it) }
                    },
                ),
            )
        }
        if (needUnits && urlSpec != null) {
            /*
             * Список серий/глав — тот же повторяющийся блок, но
             * читается иначе: заголовок там не нужен, номер выводится
             * из текста и адреса. Поэтому спека своя, а не копия
             * listing.
             */
            out = out.copy(
                units = UnitsSpec(
                    unitSelector = itemSelector,
                    fields = mapOf("url" to urlSpec, "name" to FieldSpec(selector = "a")),
                ),
            )
        }
        return out.takeIf { it != config }
    }

    /**
     * Повторяющиеся блоки документа, по убыванию правдоподобия.
     *
     * Отличие от `Healer.guessRepeatingBlock` принципиальное: там
     * берётся ПЕРВАЯ подходящая группа и только она, здесь — все
     * кандидаты с оценкой. На живом сайте меню жанров («Экшен»,
     * «Школа», …) — это тоже десятки однотипных детей со ссылками, и
     * без отсева навигации любой синтез уезжает в подвал.
     */
    internal fun repeatingGroups(dom: Dom, limit: Int = 4000): List<List<Node>> {
        val scored = ArrayList<Pair<Int, List<Node>>>()
        for (n in dom.walk(limit)) {
            val kids = n.children()
            if (kids.size < MIN_CARDS) continue
            val group = kids.groupBy { it.tag }.maxByOrNull { it.value.size }?.value ?: continue
            if (group.size < MIN_CARDS) continue

            val withContentLink = group.count { contentHrefOf(it) != null }
            if (withContentLink < group.size * 0.6) continue
            // Пункт меню — это ссылка и два слова. Карточка тайтла
            // несёт заметный текст: заголовок, тип, год.
            val meaty = group.count { it.text().length >= 20 }
            if (meaty < group.size * 0.6) continue

            scored += (withContentLink * 10 + minOf(group.size, 40)) to group
        }
        return scored.sortedByDescending { it.first }.map { it.second }
    }

    /**
     * Самый многочисленный набор классов внутри группы. Разные классы
     * при одном теге — это разные роли (карточка против врезки), и
     * карточек в каталоге по определению больше.
     */
    internal fun dominantSubgroup(group: List<Node>): List<Node> {
        val byClass = group.groupBy { Signatures.stableClassesOf(it).sorted().joinToString(".") }
        val biggest = byClass.maxByOrNull { it.value.size } ?: return group
        return if (biggest.value.size >= MIN_CARDS) biggest.value else group
    }

    private fun selectorFor(sample: Node, dom: Dom, expected: Int): String? =
        SelectorSynth.forRepeating(sample, dom, expected).firstOrNull()?.css

    /**
     * Ссылка на содержимое карточки.
     *
     * Внутри карточки почти всегда несколько ссылок: обложка, заголовок
     * и хвост из жанров. Жанры и годы ведут в каталог, а не на тайтл,
     * поэтому берётся первая ссылка, не похожая на навигацию.
     */
    internal fun contentHrefOf(card: Node): String? {
        val links = if (card.tag == "a" && card.hasAttr("href")) listOf(card) else card.select("a[href]")
        for (a in links) {
            val href = a.absUrl("href").ifBlank { a.attr("href") }
            if (href.isBlank()) continue
            if (NON_HTTP.containsMatchIn(href)) continue
            if (isSectionLink(href)) continue
            return href
        }
        return null
    }

    /**
     * Ссылка ведёт в рубрику, а не на содержимое.
     *
     * Признак — путь целиком состоит из служебных слов и чисел:
     * `/genres/action/`, `/year/2024/`, `/type/tv/`. Как только в нём
     * появляется сегмент с собственным именем (`8376-dark-moon.html`),
     * это уже страница тайтла, в какой бы рубрике она ни лежала.
     */
    internal fun isSectionLink(href: String): Boolean {
        val path = href.substringAfter("://", href).substringAfter('/', "")
            .substringBefore('?').substringBefore('#')
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) return true
        if (segments.none { it.lowercase() in SECTION_WORDS }) return false

        /*
         * Путь начинается с рубрики — значит решает то, есть ли в нём
         * сегмент, опознающий конкретную вещь. У страницы тайтла это
         * почти всегда числовой идентификатор в слаге
         * (`8376-dark-moon`) или расширение (`.html`); у рубрики —
         * только слово (`action`, `tv`). Проверка по содержимому, а не
         * по длине пути: `/type/tv/` и `/type/tv/8376-x.html` в
         * остальном неразличимы.
         */
        return segments.none { s ->
            s.endsWith(".html", ignoreCase = true) ||
                (s.any { it.isDigit() } && s.any { it == '-' || it == '_' })
        }
    }

    private fun urlFieldOf(card: Node): FieldSpec? {
        val href = contentHrefOf(card) ?: return null
        val link = (if (card.tag == "a") listOf(card) else card.select("a[href]"))
            .firstOrNull { (it.absUrl("href").ifBlank { it.attr("href") }) == href } ?: return null
        val sel = relativeSelector(link, card) ?: return null
        return FieldSpec(selector = sel, transform = ValueTransform.HREF, required = true)
    }

    /**
     * Заголовок карточки: самый крупный заголовочный тег, а при его
     * отсутствии — узел с самым длинным собственным текстом. Второе
     * важнее, чем кажется: сетки на div-ах без h1..h6 встречаются
     * не реже.
     */
    private fun titleFieldOf(card: Node): FieldSpec? {
        for (tag in listOf("h1", "h2", "h3", "h4")) {
            if (card.selectFirst(tag)?.text()?.isNotBlank() == true) {
                return FieldSpec(selector = tag, required = true)
            }
        }
        val best = card.descendants()
            .filter { it.children().isEmpty() && it.ownText().length in 3..200 }
            .maxByOrNull { it.ownText().length } ?: return null
        val sel = relativeSelector(best, card) ?: return null
        return FieldSpec(selector = sel, required = true)
    }

    private fun coverFieldOf(card: Node): FieldSpec? {
        val img = card.selectFirst("img[src]") ?: return null
        val sel = relativeSelector(img, card) ?: return null
        return FieldSpec(selector = sel, transform = ValueTransform.SRC)
    }

    /**
     * Селектор узла ОТНОСИТЕЛЬНО карточки.
     *
     * Поля читаются в области карточки, поэтому селектор с привязкой к
     * документу здесь бесполезен: он либо не найдёт ничего, либо
     * поймает чужую карточку.
     */
    internal fun relativeSelector(node: Node, card: Node): String? {
        if (node === card) return null
        val classes = Signatures.stableClassesOf(node)
        val candidates = buildList {
            if (classes.isNotEmpty()) {
                add("${node.tag}.${classes.first()}")
                add(".${classes.first()}")
            }
            add(node.tag)
        }
        return candidates.firstOrNull { css ->
            core.parse.Css.isValid(css) && card.select(css).firstOrNull() === node
        } ?: candidates.firstOrNull { css ->
            core.parse.Css.isValid(css) && card.select(css).isNotEmpty()
        }
    }
}
