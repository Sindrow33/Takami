package core.extract

import core.model.*
import core.parse.Css
import core.parse.Dom
import core.parse.Node
import core.parse.ValueReader

enum class RequestKind { LISTING, ENTRY, UNITS, CONTENT }

class StandardExtractor {

    fun extract(kind: RequestKind, dom: Dom, config: SourceConfig, url: String = dom.baseUri): Extracted = when (kind) {
        RequestKind.LISTING -> listing(dom, config)
        RequestKind.ENTRY -> entry(dom, config)
        RequestKind.UNITS -> units(dom, config)
        RequestKind.CONTENT -> Extracted(
            ParsedPayload.Content(
                TerminalContent.Unavailable(BlockReason.UNKNOWN, "терминальный слой не подключён")
            ),
            ExtractionTrace(),
        )
    }

    /* --- список ------------------------------------------------------ */

    private fun listing(dom: Dom, config: SourceConfig): Extracted {
        val spec = config.listing
            ?: return Extracted(ParsedPayload.Listing(emptyList()), ExtractionTrace())

        val (rung, cards) = pickRung(spec.itemLadder) { dom.select(it) }
        val rungs = HashMap<String, Int>()
        val items = ArrayList<MediaItem>(cards.size)
        val seen = HashSet<String>()
        var dropped = 0
        var dupes = 0

        for (card in cards) {
            val values = readFields(card, spec.fields, rungs)
            val url = values["url"] ?: hrefOf(card)
            val title = Clean.title(values["title"] ?: "")
            if (url.isNullOrBlank() || title.isBlank()) { dropped++; continue }

            val key = KeyMaker.fromUrl(url)
            if (!seen.add(key)) dupes++

            items += MediaItem(
                key = key,
                title = title,
                url = url,
                cover = values["cover"]?.takeIf { !ValueReader.looksLikePlaceholder(it) },
                extras = values.filterKeys { it !in CORE_LISTING }.filterValues { it.isNotBlank() },
            )
        }

        val next = spec.nextPageSelector
            ?.takeIf { Css.isValid(it) }
            ?.let { dom.selectFirst(it) }
            ?.let { it.absUrl("href").ifBlank { it.attr("href") } }
            ?.takeIf { it.isNotBlank() }

        val trace = ExtractionTrace(
            itemRung = rung,
            fieldRungs = rungs,
            emptyFields = spec.fields.keys.filter { f -> items.none { !it.field(f).isNullOrBlank() } }.toSet(),
            duplicateKeys = dupes,
            droppedItems = dropped,
        )
        return Extracted(ParsedPayload.Listing(items, next), trace)
    }

    /* --- карточка ----------------------------------------------------- */

    private fun entry(dom: Dom, config: SourceConfig): Extracted {
        val spec = config.entry ?: EntrySpec()
        val rungs = HashMap<String, Int>()
        val v = readFields(dom.root, spec.fields, rungs)

        val title = Clean.title(
            v["title"]
                ?: metaContent(dom, "og:title")
                ?: dom.title.orEmpty()
        )
        val url = v["url"] ?: dom.baseUri
        val inner = config.units?.let { units(dom, config) }

        val e = MediaEntry(
            key = KeyMaker.fromUrl(url),
            title = title,
            url = url,
            cover = (v["cover"] ?: metaContent(dom, "og:image"))
                ?.takeIf { !ValueReader.looksLikePlaceholder(it) },
            description = v["description"] ?: metaContent(dom, "og:description"),
            units = (inner?.payload as? ParsedPayload.Units)?.units.orEmpty(),
            extras = v.filterKeys { it !in CORE_ENTRY }.filterValues { it.isNotBlank() },
        )

        val innerTrace = inner?.trace
        val trace = ExtractionTrace(
            itemRung = innerTrace?.itemRung ?: 0,
            fieldRungs = rungs + (innerTrace?.fieldRungs ?: emptyMap()),
            emptyFields = if (title.isBlank()) setOf("title") else emptySet(),
            duplicateKeys = innerTrace?.duplicateKeys ?: 0,
            droppedItems = innerTrace?.droppedItems ?: 0,
        )
        return Extracted(ParsedPayload.Entry(e), trace)
    }

    /* --- серии / главы ------------------------------------------------ */

    private fun units(dom: Dom, config: SourceConfig): Extracted {
        val spec = config.units
            ?: return Extracted(ParsedPayload.Units(emptyList()), ExtractionTrace())

        val (rung, nodes) = pickRung(spec.unitLadder) { dom.select(it) }
        val rungs = HashMap<String, Int>()
        val out = ArrayList<MediaUnit>(nodes.size)
        val seen = HashSet<String>()
        var dropped = 0
        var dupes = 0

        for (n in nodes) {
            val v = readFields(n, spec.fields, rungs)
            val url = v["url"] ?: hrefOf(n)
            if (url.isNullOrBlank()) { dropped++; continue }
            val key = KeyMaker.fromUrl(url)
            if (!seen.add(key)) dupes++
            out += MediaUnit(
                key = key,
                url = url,
                number = UnitNumber.resolve(v["number"], v["name"], n.text(), url),
                name = v["name"]?.let { Clean.text(it) },
                extras = v.filterKeys { it !in CORE_UNITS }.filterValues { it.isNotBlank() },
            )
        }

        val ordered = orderUnits(out, spec.reverseOrder)
        val trace = ExtractionTrace(
            itemRung = rung,
            fieldRungs = rungs,
            duplicateKeys = dupes,
            droppedItems = dropped,
        )
        return Extracted(ParsedPayload.Units(ordered), trace)
    }

    private fun orderUnits(list: List<MediaUnit>, reverse: Boolean): List<MediaUnit> {
        if (list.isEmpty()) return list
        val numbered = list.count { it.number != null }
        return when {
            numbered >= list.size * 0.8 -> list.sortedBy { it.number ?: Double.MAX_VALUE }
            reverse -> list.asReversed()
            else -> list
        }
    }

    /* --- общее -------------------------------------------------------- */

    private fun readFields(
        scope: Node,
        specs: Map<String, FieldSpec>,
        rungs: MutableMap<String, Int>,
    ): Map<String, String> {
        val out = HashMap<String, String>(specs.size)
        for ((name, spec) in specs) {
            var value: String? = null
            var used = 0
            for ((i, sel) in spec.ladder.withIndex()) {
                if (!Css.isValid(sel)) continue
                val hit = scope.select(sel).ifEmpty {
                    if (scope.matches(sel)) listOf(scope) else emptyList()
                }
                if (hit.isEmpty()) continue
                val read = if (spec.transform.joinSeparator != null)
                    ValueReader.readAll(hit, spec.transform)
                else ValueReader.read(hit.first(), spec.transform)
                if (!read.isNullOrBlank()) { value = read; used = i; break }
            }
            if (value != null) {
                out[name] = Clean.text(value)
                rungs[name] = maxOf(rungs[name] ?: 0, used)
            }
        }
        return out
    }

    private fun pickRung(ladder: List<String>, select: (String) -> List<Node>): Pair<Int, List<Node>> {
        for ((i, sel) in ladder.withIndex()) {
            if (!Css.isValid(sel)) continue
            val hit = select(sel)
            if (hit.isNotEmpty()) return i to hit
        }
        return 0 to emptyList()
    }

    private fun hrefOf(node: Node): String? {
        val a = if (node.tag == "a" && node.hasAttr("href")) node else node.selectFirst("a[href]")
        val raw = a?.let { it.absUrl("href").ifBlank { it.attr("href") } }
        return raw?.takeIf { it.isNotBlank() && !it.startsWith("javascript:") && it != "#" }
    }

    private fun metaContent(dom: Dom, property: String): String? =
        (dom.selectFirst("meta[property=\"$property\"]") ?: dom.selectFirst("meta[name=\"$property\"]"))
            ?.attr("content")?.takeIf { it.isNotBlank() }

    private companion object {
        val CORE_LISTING = setOf("title", "url", "cover")
        val CORE_ENTRY = setOf("title", "url", "cover", "description")
        val CORE_UNITS = setOf("url", "number", "name")
    }
}

/* --- номера серий/глав ------------------------------------------------ */

object UnitNumber {
    private val EXPLICIT = Regex("""^\s*(\d{1,4})(?:[.,](\d{1,2}))?\s*$""")
    private val LABELLED = Regex("""(?:серия|эпизод|глава|том|episode|ep|chapter|ch)\s*[.№#]?\s*(\d{1,4})(?:[.,](\d{1,2}))?""", RegexOption.IGNORE_CASE)
    private val TRAILING = Regex("""(\d{1,4})(?:[.,](\d{1,2}))?\s*(?:серия|эпизод|глава|том)""", RegexOption.IGNORE_CASE)
    private val IN_URL = Regex("""(?:episode|ep|series|chapter|ch|glava|seriya)[-_/]?(\d{1,4})""", RegexOption.IGNORE_CASE)
    private val TAIL = Regex("""(\d{1,4})(?:[.,](\d{1,2}))?/?$""")

    fun resolve(explicit: String?, name: String?, nodeText: String?, url: String?): Double? {
        explicit?.let { s -> parse(EXPLICIT, s)?.let { return it } }
        name?.let { s -> parse(LABELLED, s)?.let { return it } ?: parse(TRAILING, s)?.let { return it } }
        nodeText?.takeIf { it.length <= 160 }?.let { s ->
            parse(LABELLED, s)?.let { return it }
            parse(TRAILING, s)?.let { return it }
        }
        url?.let { s ->
            parse(IN_URL, s)?.let { return it }
            parse(TAIL, s)?.let { if (it < 3000) return it }
        }
        return null
    }

    private fun parse(re: Regex, s: String): Double? {
        val m = re.find(s) ?: return null
        val whole = m.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val frac = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
        return if (frac == null) whole.toDouble() else "$whole.$frac".toDoubleOrNull()
    }
}

/* --- чистка значений --------------------------------------------------- */

object Clean {
    private val WS = Regex("""\s+""")
    private val JUNK = Regex("""\s*[|—–-]\s*(смотреть онлайн|читать онлайн|онлайн|watch online|read online).*$""", RegexOption.IGNORE_CASE)

    fun text(s: String): String =
        s.replace('\u00A0', ' ')
            .replace("\u200B", "")
            .replace("\uFEFF", "")
            .replace(WS, " ")
            .trim()

    fun title(s: String): String = JUNK.replace(text(s), "").trim()
}
