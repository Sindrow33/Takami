package core.parse

/**
 * Лёгкие операции с адресами без обращения к сети.
 *
 * Раньше существовали два объекта с этим именем — в `core.parse` и в
 * `core.net` — с разными реализациями `looksLikeUrl`: первый считал
 * ссылкой строку с точкой, второй нет. Валидатор и терминальный слой
 * из-за этого расходились в оценке одних и тех же значений.
 * Здесь единственная версия; сетевой слой пользуется ей же.
 */
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

    /** Строгий вариант для сетевого слоя: только явные ссылки. */
    fun isAbsoluteOrRooted(value: String): Boolean {
        val v = value.trim()
        if (v.length < 4 || v.contains(' ')) return false
        return v.startsWith("http://") || v.startsWith("https://") ||
            v.startsWith("//") || v.startsWith("/")
    }

    fun absolutize(url: String, base: String): String {
        val u = url.trim()
        if (u.isEmpty()) return u
        return when {
            u.startsWith("http://") || u.startsWith("https://") -> u
            u.startsWith("//") -> (schemeOf(base) ?: "https") + ":" + u
            u.startsWith("data:") || u.startsWith("blob:") -> u
            else -> runCatching { java.net.URI(base).resolve(u).toString() }.getOrDefault(u)
        }
    }

    fun schemeOf(url: String): String? = runCatching { java.net.URI(url).scheme }.getOrNull()

    /** Одна ли это площадка. Нужно для фильтрации картинок по хосту. */
    fun sameSite(a: String, b: String): Boolean {
        val ha = registrable(a) ?: return false
        val hb = registrable(b) ?: return false
        return ha == hb
    }

    /** Грубая «регистрируемая» часть: последние два уровня домена. */
    private fun registrable(url: String): String? {
        val host = runCatching { java.net.URI(url).host }.getOrNull() ?: return null
        val parts = host.removePrefix("www.").split('.')
        return if (parts.size <= 2) host.removePrefix("www.")
        else parts.takeLast(2).joinToString(".")
    }
}
