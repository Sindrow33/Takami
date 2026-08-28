package moe.scenesearch.resolver

data class NormalizedTitle(
    val core: String,
    val tokens: Set<String>,
    val season: Int?,
)

/**
 * Строки "Оверлорд 3 сезон", "Overlord III" и "Overlord 3rd Season" сводятся
 * к общему ядру плюс отдельно вынесенный номер сезона.
 * Без этого третий сезон стабильно уезжает в первый.
 */
object TitleNormalizer {

    private val seasonPatterns = listOf(
        Regex("""(\d+)\s*-?\s*[ийя]?\s*сезон"""),
        Regex("""сезон\s*(\d+)"""),
        Regex("""season\s*(\d+)"""),
        Regex("""(\d+)\s*(?:st|nd|rd|th)\s+season"""),
        Regex("""\bs(\d{1,2})\b"""),
        Regex("""\btv\s*-?\s*(\d)\b"""),
    )

    private val roman = mapOf("ii" to 2, "iii" to 3, "iv" to 4, "v" to 5, "vi" to 6)

    private val stopWords = setOf(
        "the", "a", "an", "tv", "season", "сезон", "часть", "part", "series", "аниме",
    )

    fun normalize(raw: String): NormalizedTitle {
        var s = raw.lowercase().replace('ё', 'е')
        var season: Int? = null

        for (p in seasonPatterns) {
            val m = p.find(s)
            if (m != null) {
                season = m.groupValues[1].toIntOrNull()
                s = s.removeRange(m.range)
                break
            }
        }

        s = s.replace(Regex("""[^\p{L}\p{Nd}]+"""), " ").trim()
        val tokens = s.split(' ').filter { it.isNotBlank() }.toMutableList()

        if (season == null && tokens.isNotEmpty()) {
            val r = roman[tokens[tokens.size - 1]]
            if (r != null) {
                season = r
                tokens.removeAt(tokens.size - 1)
            }
        }

        val clean = tokens.filter { it !in stopWords }
        return NormalizedTitle(clean.joinToString(" "), clean.toSet(), season)
    }
}
