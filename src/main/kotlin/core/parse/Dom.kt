package core.parse

import core.model.ValueTransform
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Selector

/* =====================================================================
 * 1. ИНТЕРФЕЙСЫ
 * ===================================================================== */

/**
 * Узел дерева. Намеренно уже, чем Element из Jsoup: движок не должен
 * уметь менять документ, только читать. Это не паранойя — при обходе
 * в ре-поиске случайная мутация дерева ломает уже посчитанные индексы.
 */
interface Node {
    val tag: String
    val id: String?
    val classes: List<String>
    val attrs: Map<String, String>

    /** Глубина от корня документа. Нужна сигнатурам и оценке селекторов. */
    val depth: Int

    /** Позиция среди всех детей родителя, 0-based. */
    val indexInParent: Int

    /** Сколько братьев того же тега, включая себя. */
    val siblingsOfSameTag: Int

    fun attr(name: String): String
    fun hasAttr(name: String): Boolean

    /** Разрешает относительный URL относительно baseUri документа. */
    fun absUrl(attrName: String): String

    /** Текст всего поддерева, схлопнутые пробелы. */
    fun text(): String

    /** Только собственные текстовые узлы, без детей. */
    fun ownText(): String

    fun html(): String
    fun outerHtml(): String

    fun select(css: String): List<Node>
    fun selectFirst(css: String): Node?
    fun matches(css: String): Boolean

    fun parent(): Node?
    fun children(): List<Node>

    /** Теги соседей слева и справа — дешёвый структурный отпечаток. */
    fun neighbourTags(radius: Int = 2): List<String>

    /** Все предки от ближайшего к корню. */
    fun ancestors(limit: Int = 24): List<Node>

    /** Поддерево в порядке обхода, включая себя. Ленивое — обход дорог. */
    fun descendants(): Sequence<Node>

    /** Число элементов в поддереве. Используется в оценке плотности текста. */
    fun elementCount(): Int
}

/**
 * Документ. Помимо селекторов даёт то, чего в Jsoup нет из коробки,
 * но что нужно лечению: обход с жёстким лимитом и поиск по тексту.
 */
interface Dom {
    val baseUri: String
    val root: Node
    val htmlLength: Int
    val title: String?

    fun select(css: String): List<Node>
    fun selectFirst(css: String): Node?

    /**
     * Обход в ширину с лимитом. ReSearchBudget разрешает 4000 узлов —
     * без явного лимита на тяжёлой странице обход съест весь бюджет.
     */
    fun walk(limit: Int): Sequence<Node>

    /**
     * Узлы, чей СОБСТВЕННЫЙ текст совпадает с образцом. Именно собственный:
     * иначе для "Магическая битва" вернётся и <body>, и все обёртки над ним.
     */
    fun findByOwnText(text: String, limit: Int = 8, exact: Boolean = true): List<Node>

    /** Узлы, у которых значение атрибута совпадает. Для поиска по golden url. */
    fun findByAttrValue(attrName: String, value: String, limit: Int = 8): List<Node>

    /** Сырой HTML — нужен терминальному слою для regex-поиска по скриптам. */
    fun rawHtml(): String

    /** Содержимое всех <script> без атрибута src. */
    fun inlineScripts(): List<String>
}

interface Parser {
    fun parse(html: String, baseUri: String): Dom
}

/* =====================================================================
 * 2. БЕЗОПАСНЫЕ СЕЛЕКТОРЫ
 * ===================================================================== */

/**
 * Селекторы приходят из трёх источников: поставка, синтезатор пикера,
 * ре-поиск. Два последних генерируют строки автоматически, и невалидный
 * CSS — вопрос времени. Ни один такой случай не должен ронять парсинг.
 */
object Css {

    private val validated = HashMap<String, Boolean>()

    fun isValid(css: String): Boolean {
        if (css.isBlank()) return false
        validated[css]?.let { return it }
        val ok = runCatching { Selector.select(css, EMPTY_DOC) }.isSuccess
        if (validated.size < CACHE_CAP) validated[css] = ok
        return ok
    }

    /** Первый валидный селектор из лестницы. */
    fun firstValid(ladder: List<String>): String? = ladder.firstOrNull { isValid(it) }

    private val EMPTY_DOC: Document by lazy { Jsoup.parse("<html><body></body></html>") }
    private const val CACHE_CAP = 512
}

/* =====================================================================
 * 3. РЕАЛИЗАЦИЯ НА JSOUP
 * ===================================================================== */

class JsoupParser : Parser {
    override fun parse(html: String, baseUri: String): Dom = JsoupDom(Jsoup.parse(html, baseUri), html)
}

internal class JsoupNode(
    private val el: Element,
    private val doc: JsoupDom?,
) : Node {

    override val tag: String get() = el.tagName()
    override val id: String? get() = el.id().takeIf { it.isNotBlank() }

    override val classes: List<String> by lazy {
        el.classNames().filter { it.isNotBlank() }.take(MAX_CLASSES)
    }

    override val attrs: Map<String, String> by lazy {
        el.attributes().asList()
            .filter { it.key != "style" && it.value.length <= MAX_ATTR_LEN }
            .associate { it.key to it.value }
    }

    override val depth: Int by lazy { el.parents().size }

    override val indexInParent: Int get() = el.elementSiblingIndex()

    override val siblingsOfSameTag: Int by lazy {
        el.parent()?.children()?.count { it.tagName() == el.tagName() } ?: 1
    }

    override fun attr(name: String): String = el.attr(name)
    override fun hasAttr(name: String): Boolean = el.hasAttr(name)

    override fun absUrl(attrName: String): String {
        val abs = el.absUrl(attrName)
        return abs.ifBlank { el.attr(attrName) }
    }

    private val cachedText: String by lazy { el.text().trim() }
    override fun text(): String = cachedText

    override fun ownText(): String = el.ownText().trim()
    override fun html(): String = el.html()
    override fun outerHtml(): String = el.outerHtml()

    override fun select(css: String): List<Node> =
        if (!Css.isValid(css)) emptyList()
        else runCatching { el.select(css).map { JsoupNode(it, doc) } }.getOrElse { emptyList() }

    override fun selectFirst(css: String): Node? =
        if (!Css.isValid(css)) null
        else runCatching { el.selectFirst(css)?.let { JsoupNode(it, doc) } }.getOrNull()

    override fun matches(css: String): Boolean =
        Css.isValid(css) && runCatching { el.`is`(css) }.getOrDefault(false)

    override fun parent(): Node? = el.parent()?.let { JsoupNode(it, doc) }

    override fun children(): List<Node> = el.children().map { JsoupNode(it, doc) }

    override fun neighbourTags(radius: Int): List<String> {
        val p = el.parent() ?: return emptyList()
        val kids = p.children()
        val i = el.elementSiblingIndex()
        val from = (i - radius).coerceAtLeast(0)
        val to = (i + radius).coerceAtMost(kids.size - 1)
        return (from..to).filter { it != i }.map { kids[it].tagName() }
    }

    override fun ancestors(limit: Int): List<Node> =
        el.parents().take(limit).map { JsoupNode(it, doc) }.reversed()

    override fun descendants(): Sequence<Node> = sequence {
        yield(this@JsoupNode)
        for (child in el.getAllElements()) {
            if (child !== el) yield(JsoupNode(child, doc))
        }
    }

    override fun elementCount(): Int = el.getAllElements().size

    override fun equals(other: Any?): Boolean = other is JsoupNode && other.el === el
    override fun hashCode(): Int = System.identityHashCode(el)
    override fun toString(): String = "<$tag${id?.let { "#$it" } ?: ""}${classes.joinToString("") { ".$it" }}>"

    internal fun unwrap(): Element = el

    companion object {
        private const val MAX_CLASSES = 12
        private const val MAX_ATTR_LEN = 512
    }
}

internal class JsoupDom(
    private val doc: Document,
    private val raw: String,
) : Dom {

    override val baseUri: String get() = doc.baseUri()
    override val root: Node by lazy { JsoupNode(doc.body() ?: doc, this) }
    override val htmlLength: Int get() = raw.length
    override val title: String? get() = doc.title().takeIf { it.isNotBlank() }

    /**
     * Один и тот же itemSelector за запрос выбирается минимум трижды:
     * экстрактором, валидатором заполненности и проверкой прототипов.
     * На каталоге с тысячей узлов это заметная разница.
     */
    private val selectCache = HashMap<String, List<Node>>()

    override fun select(css: String): List<Node> {
        selectCache[css]?.let { return it }
        if (!Css.isValid(css)) return emptyList()
        val result = runCatching { doc.select(css).map { JsoupNode(it, this) } }.getOrElse { emptyList() }
        if (selectCache.size < SELECT_CACHE_CAP) selectCache[css] = result
        return result
    }

    override fun selectFirst(css: String): Node? = select(css).firstOrNull()

    override fun walk(limit: Int): Sequence<Node> = sequence {
        var seen = 0
        for (el in doc.getAllElements()) {
            if (seen++ >= limit) break
            yield(JsoupNode(el, this@JsoupDom))
        }
    }

    override fun findByOwnText(text: String, limit: Int, exact: Boolean): List<Node> {
        val needle = normalize(text)
        if (needle.length < MIN_TEXT_MATCH) return emptyList()
        val out = ArrayList<Node>(limit)
        for (el in doc.getAllElements()) {
            if (out.size >= limit) break
            // Быстрый отсев: если поддерево вообще не содержит текст — пропускаем.
            if (!el.text().contains(needle, ignoreCase = true)) continue
            val own = normalize(ownTextOf(el))
            val hit = if (exact) own.equals(needle, ignoreCase = true)
                      else own.contains(needle, ignoreCase = true)
            if (hit) out += JsoupNode(el, this)
        }
        return out
    }

    override fun findByAttrValue(attrName: String, value: String, limit: Int): List<Node> {
        val out = ArrayList<Node>(limit)
        for (el in doc.getAllElements()) {
            if (out.size >= limit) break
            if (!el.hasAttr(attrName)) continue
            val v = el.attr(attrName)
            if (v == value || el.absUrl(attrName) == value) out += JsoupNode(el, this)
        }
        return out
    }

    override fun rawHtml(): String = raw

    override fun inlineScripts(): List<String> = doc.select("script:not([src])").map { it.data() }

    private fun ownTextOf(el: Element): String = buildString {
        for (n in el.childNodes()) if (n is TextNode) append(n.wholeText)
    }

    private fun normalize(s: String): String = s.replace(WS, " ").trim()

    companion object {
        private val WS = Regex("\\s+")
        private const val SELECT_CACHE_CAP = 64
        private const val MIN_TEXT_MATCH = 3
    }
}

/* =====================================================================
 * 4. ЧТЕНИЕ ЗНАЧЕНИЯ
 * ===================================================================== */

/**
 * Единственное место, где ValueTransform превращается в строку.
 * Раньше это было размазано по экстрактору, пикеру и валидатору —
 * и они по-разному обрабатывали ленивые картинки.
 */
object ValueReader {

    /** Порядок важен: реальный URL чаще в data-src, чем в src. */
    private val LAZY_ATTRS = listOf(
        "data-src", "data-original", "data-lazy-src", "data-lazy", "data-echo", "src",
    )

    private val PLACEHOLDER_HINTS = listOf(
        "data:image", "placeholder", "blank.gif", "lazy.png", "spacer", "1x1", "loading.svg",
    )

    fun read(node: Node, transform: ValueTransform): String? {
        val raw = when {
            transform.attr == null -> if (transform.stripHtml) node.text() else node.html()
            transform.attr == "src" -> readImageSrc(node, transform.absoluteUrl)
            transform.attr == "srcset" -> readSrcset(node, transform.absoluteUrl)
            transform.absoluteUrl -> node.absUrl(transform.attr!!).ifBlank { node.attr(transform.attr!!) }
            else -> node.attr(transform.attr!!)
        }.trim()

        if (raw.isEmpty()) return null

        val extracted = transform.compiled?.let { re ->
            val m = re.find(raw) ?: return null
            (m.groupValues.getOrNull(1) ?: m.value).trim()
        } ?: raw

        return extracted.ifBlank { null }
    }

    /** Собирает значения из нескольких узлов — для жанров, тегов, авторов. */
    fun readAll(nodes: List<Node>, transform: ValueTransform): String? {
        val sep = transform.joinSeparator ?: return nodes.firstOrNull()?.let { read(it, transform) }
        val values = nodes.mapNotNull { read(it, transform) }.filter { it.isNotBlank() }
        return values.takeIf { it.isNotEmpty() }?.joinToString(sep)
    }

    /**
     * Ленивая загрузка: src часто содержит однопиксельную заглушку,
     * а настоящая картинка лежит в data-атрибуте. Берём первый непустой
     * непохожий на заглушку, и только потом — src каким бы он ни был.
     */
    private fun readImageSrc(node: Node, absolute: Boolean): String {
        for (a in LAZY_ATTRS) {
            if (!node.hasAttr(a)) continue
            val v = if (absolute) node.absUrl(a).ifBlank { node.attr(a) } else node.attr(a)
            if (v.isBlank()) continue
            if (looksLikePlaceholder(v)) continue
            return v
        }
        if (node.hasAttr("srcset")) return readSrcset(node, absolute)
        return if (absolute) node.absUrl("src") else node.attr("src")
    }

    /** Из srcset берём самый широкий вариант: "a.jpg 300w, b.jpg 900w" → b.jpg */
    private fun readSrcset(node: Node, absolute: Boolean): String {
        val set = node.attr("srcset").ifBlank { node.attr("data-srcset") }
        if (set.isBlank()) return ""
        val best = set.split(',')
            .mapNotNull { part ->
                val bits = part.trim().split(WS_RE)
                val url = bits.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val w = bits.getOrNull(1)?.removeSuffix("w")?.removeSuffix("x")?.toDoubleOrNull() ?: 1.0
                url to w
            }
            .maxByOrNull { it.second }?.first ?: return ""
        return if (absolute) resolve(node, best) else best
    }

    private fun resolve(node: Node, url: String): String =
        if (url.startsWith("http") || url.startsWith("//")) url
        else runCatching { java.net.URI(node.absUrl("src").ifBlank { "" }).resolve(url).toString() }
            .getOrDefault(url)

    fun looksLikePlaceholder(url: String): Boolean {
        val low = url.lowercase()
        return PLACEHOLDER_HINTS.any { low.contains(it) } || low.length < 8
    }

    private val WS_RE = Regex("\\s+")
}

/* =====================================================================
 * 5. ПЛОТНОСТЬ ТЕКСТА
 * ===================================================================== */

/**
 * Отдельно, потому что этим пользуются двое: TextResolver при поиске
 * тела главы и валидатор при проверке, не подсунули ли вместо описания
 * блок ссылок. Формула должна быть одна на обоих.
 */
object Density {

    data class Metrics(
        val textLength: Int,
        val elementCount: Int,
        val linkTextLength: Int,
        val paragraphCount: Int,
    ) {
        /** Текста на один элемент. У осмысленного текста — десятки. */
        val textPerElement: Double
            get() = if (elementCount == 0) textLength.toDouble() else textLength.toDouble() / elementCount

        /** Доля текста внутри ссылок. У навигации близка к единице. */
        val linkRatio: Double
            get() = if (textLength == 0) 1.0 else linkTextLength.toDouble() / textLength
    }

    fun of(node: Node): Metrics {
        val text = node.text()
        val links = node.select("a").sumOf { it.text().length }
        val paras = node.select("p, br").size
        return Metrics(text.length, node.elementCount(), links, paras)
    }

    /** Похоже на осмысленный связный текст, а не на список ссылок. */
    fun isProse(m: Metrics, minLength: Int = 200): Boolean =
        m.textLength >= minLength && m.linkRatio < 0.30 && m.textPerElement > 25
}
