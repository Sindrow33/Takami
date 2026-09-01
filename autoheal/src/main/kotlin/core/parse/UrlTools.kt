package core.parse

/** Лёгкие проверки строк-адресов без обращения к сети. */
object UrlTools {

    private val SCHEME = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*:""")

    fun looksLikeUrl(value: String): Boolean {
        val s = value.trim()
        if (s.isEmpty() || s.length > 2048) return false
        if (s.any { it.isWhitespace() }) return false
        return when {
            s.startsWith("http://") || s.startsWith("https://") -> s.length > 10
            s.startsWith("//") -> s.length > 4
            s.startsWith("/") -> true
            s.startsWith("javascript:") || s.startsWith("#") -> false
            SCHEME.containsMatchIn(s) -> false
            else -> s.contains('/') || s.contains('.')
        }
    }
}
