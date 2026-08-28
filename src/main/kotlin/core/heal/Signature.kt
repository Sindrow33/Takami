package core.heal

import core.model.ElementSignature
import core.parse.Dom
import core.parse.Node
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/* ═══════════════════════════════════════════════════════════════════
   ОТПЕЧАТОК ЭЛЕМЕНТА
   Селектор — адрес, отпечаток — фотография. Адрес меняется при каждом
   редизайне, внешность и окружение — почти никогда.
   ═══════════════════════════════════════════════════════════════════ */

object Signatures {

    private val WS = Regex("\\s+")
    private val TOKEN_SPLIT = Regex("[^\\p{L}\\p{N}]+")

    /** Классы, сгенерированные сборщиком: css-1q7hf2n, Button_root__x8Ky2, _3fLm9. */
    private val HASHED = listOf(
        Regex("^css-[0-9a-z]{4,}$", RegexOption.IGNORE_CASE),
        Regex("^[a-zA-Z]+_[a-zA-Z0-9]{3,}__[a-zA-Z0-9]{3,}$"),
        Regex("^_[0-9a-zA-Z]{5,}$"),
        Regex("^[0-9a-f]{8,}$", RegexOption.IGNORE_CASE),
        Regex("^[a-z]{1,4}[-_][0-9a-f]{5,}$", RegexOption.IGNORE_CASE),
    )

    /** Состояния: сегодня active, завтра нет. Для опознания бесполезны. */
    private val STATEFUL = Regex("^(is|has|js)-|^(active|open|show|shown|hidden|selected|current|loading)$")

    val STABLE_KEYS = listOf("id", "data-testid", "data-id", "itemprop", "name", "role", "rel")

    fun isVolatileClass(c: String): Boolean {
        if (c.isBlank() || c.length > 40) return true
        if (STATEFUL.containsMatchIn(c)) return true
        if (HASHED.any { it.matches(c) }) return true
        val digits = c.count { it.isDigit() }
        return digits >= 4 && digits.toDouble() / c.length > 0.4
    }

    fun normalizeText(s: String): String = s.trim().replace(WS, " ").lowercase()

    fun tokenize(s: String): List<String> =
        s.split(TOKEN_SPLIT).filter { it.length > 1 }.take(24)

    fun stableClassesOf(n: Node): List<String> =
        n.classes.filter { !isVolatileClass(it) }.take(6)

    /** Форма поддерева: a(img,div(h3,span)). Переживает переименование классов. */
    fun shapeOf(n: Node, maxDepth: Int = 3): String =
        StringBuilder().also { writeShape(n, maxDepth, it) }.toString()

    private fun writeShape(n: Node, depth: Int, sb: StringBuilder) {
        sb.append(n.tag)
        if (depth <= 0) return
        val kids = n.children().take(8)
        if (kids.isEmpty()) return
        sb.append('(')
        kids.forEachIndexed { i, k ->
            if (i > 0) sb.append(',')
            writeShape(k, depth - 1, sb)
        }
        sb.append(')')
    }

    private fun pathOf(n: Node): List<String> =
        n.ancestors(24).map { it.tag } + n.tag

    fun capture(node: Node, now: Long = 0L): ElementSignature {
        val text = normalizeText(node.ownText().ifBlank { node.text() })
        return ElementSignature(
            tag = node.tag,
            stableClasses = stableClassesOf(node),
            stableAttrs = STABLE_KEYS.mapNotNull { k ->
                node.attr(k).takeIf { it.isNotBlank() && it.length < 64 }?.let { k to it }
            }.toMap(),
            normalizedText = text.take(120),
            textTokens = tokenize(text),
            tagPath = pathOf(node),
            shape = shapeOf(node),
            neighbourTags = node.neighbourTags(2),
            ancestorClasses = node.ancestors(2).flatMap { stableClassesOf(it) }.take(8),
            indexInParent = node.indexInParent,
            depth = node.depth,
            capturedAt = now,
        )
    }
}

fun ElementSignature.confirm(now: Long): ElementSignature =
    copy(confirmed = confirmed + 1, capturedAt = now)

/* ═══════════════════════════════════════════════════════════════════
   СХОДСТВО
   Ни один признак не решает сам. Совпал тег — ничего не значит.
   Совпали форма, текст, окружение и позиция — это он.
   ═══════════════════════════════════════════════════════════════════ */

object Similarity {

    private const val W_TAG = 0.10
    private const val W_ATTR = 0.18
    private const val W_TEXT = 0.20
    private const val W_CLASS = 0.12
    private const val W_PATH = 0.13
    private const val W_SHAPE = 0.12
    private const val W_NEIGH = 0.08
    private const val W_POS = 0.07

    /** Признака нет у обоих: не наказываем и не награждаем. */
    private const val NEUTRAL = 0.5

    const val ACCEPT = 0.62   // ниже — считаем, что не нашли
    const val STRONG = 0.85   // выше — принимаем даже при близком втором
    const val MARGIN = 0.08   // минимальный отрыв от второго кандидата

    fun score(sig: ElementSignature, node: Node): Double = score(sig, Signatures.capture(node))

    fun score(a: ElementSignature, b: ElementSignature): Double {
        val tag = when {
            a.tag == b.tag -> 1.0
            interchangeable(a.tag, b.tag) -> 0.6
            else -> 0.0
        }

        val attr = if (a.stableAttrs.isEmpty()) NEUTRAL
        else a.stableAttrs.count { (k, v) -> b.stableAttrs[k] == v }.toDouble() / a.stableAttrs.size

        val text = when {
            a.normalizedText.isEmpty() && b.normalizedText.isEmpty() -> NEUTRAL
            a.normalizedText.isEmpty() || b.normalizedText.isEmpty() -> 0.0
            a.normalizedText == b.normalizedText -> 1.0
            else -> max(
                jaccard(a.textTokens.toSet(), b.textTokens.toSet()),
                lengthAffinity(a.normalizedText, b.normalizedText) * 0.4,
            )
        }

        val cls = if (a.stableClasses.isEmpty() && b.stableClasses.isEmpty()) NEUTRAL
        else jaccard(a.stableClasses.toSet(), b.stableClasses.toSet())

        val path = suffixOverlap(a.tagPath, b.tagPath)
        val shape = shapeAffinity(a.shape, b.shape)
        val neigh = jaccard(a.neighbourTags.toSet(), b.neighbourTags.toSet())

        val pos = 1.0 / (1.0 + abs(a.indexInParent - b.indexInParent)) *
                  (1.0 / (1.0 + abs(a.depth - b.depth) * 0.5))

        return (W_TAG * tag + W_ATTR * attr + W_TEXT * text + W_CLASS * cls +
                W_PATH * path + W_SHAPE * shape + W_NEIGH * neigh + W_POS * pos)
            .coerceIn(0.0, 1.0)
    }

    private fun interchangeable(x: String, y: String): Boolean {
        val pairs = setOf(
            setOf("div", "span"), setOf("a", "button"), setOf("h1", "h2"),
            setOf("h2", "h3"), setOf("h3", "h4"), setOf("p", "div"),
            setOf("img", "picture"), setOf("li", "div"), setOf("article", "div"),
        )
        return setOf(x, y) in pairs
    }

    fun <T> jaccard(a: Set<T>, b: Set<T>): Double {
        if (a.isEmpty() && b.isEmpty()) return NEUTRAL
        val inter = a.count { it in b }
        val union = a.size + b.size - inter
        return if (union == 0) 0.0 else inter.toDouble() / union
    }

    /** Хвост пути важнее начала: верхние обёртки меняют чаще внутренностей. */
    private fun suffixOverlap(a: List<String>, b: List<String>): Double {
        var i = a.lastIndex; var j = b.lastIndex; var same = 0
        while (i >= 0 && j >= 0 && a[i] == b[j]) { same++; i--; j-- }
        return (same.toDouble() / max(a.size, b.size).coerceAtLeast(1)).coerceAtMost(1.0)
    }

    private fun shapeAffinity(a: String, b: String): Double = when {
        a.isEmpty() && b.isEmpty() -> NEUTRAL
        a == b -> 1.0
        else -> jaccard(a.split(Regex("[(),]")).filter { it.isNotBlank() }.toSet(),
                        b.split(Regex("[(),]")).filter { it.isNotBlank() }.toSet())
    }

    private fun lengthAffinity(a: String, b: String): Double =
        min(a.length, b.length).toDouble() / max(a.length, b.length).toDouble().coerceAtLeast(1.0)
}

/* ═══════════════════════════════════════════════════════════════════
   ПОИСК ПО ОТПЕЧАТКУ
   ═══════════════════════════════════════════════════════════════════ */

data class Match(
    val node: Node,
    val score: Double,
    val runnerUp: Double,
) {
    /** Отрыв от второго кандидата: без него высокий балл ничего не гарантирует. */
    val margin: Double get() = score - runnerUp
    val isConfident: Boolean get() =
        score >= Similarity.STRONG || (score >= Similarity.ACCEPT && margin >= Similarity.MARGIN)
}

object SignatureSearch {

    fun bestMatch(sig: ElementSignature, candidates: List<Node>): Match? {
        var best: Node? = null
        var bestScore = 0.0
        var runnerUp = 0.0
        for (n in candidates) {
            val s = Similarity.score(sig, n)
            if (s > bestScore) { runnerUp = bestScore; bestScore = s; best = n }
            else if (s > runnerUp) runnerUp = s
        }
        val node = best ?: return null
        return Match(node, bestScore, runnerUp)
    }

    /** Пул кандидатов: сначала по стабильному атрибуту, потом по тегу, потом всё подряд. */
    fun pool(dom: Dom, sig: ElementSignature, limit: Int = 1200): List<Node> {
        sig.stableAttrs.entries.firstOrNull()?.let { (k, v) ->
            val byAttr = dom.findByAttrValue(k, v, limit = 64)
            if (byAttr.isNotEmpty()) return byAttr
        }
        val byTag = runCatching { dom.select(sig.tag) }.getOrDefault(emptyList())
        if (byTag.isNotEmpty() && byTag.size <= 800) return byTag
        return dom.walk(limit).toList()
    }

    fun find(dom: Dom, sig: ElementSignature): Match? =
        bestMatch(sig, pool(dom, sig))?.takeIf { it.isConfident }
}
