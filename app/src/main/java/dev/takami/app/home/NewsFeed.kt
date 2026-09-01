package dev.takami.app.home

import dev.takami.app.news.DiscoveredNews
import java.util.Locale

/**
 * Превращение разобранных автопарсером новостей в карточки карусели.
 *
 * Вынесено отдельно от экрана и без единого обращения к Android: это
 * единственное место, где решается, что показать в подписи и как
 * назвать возраст новости, и его дешевле проверить тестом, чем
 * скриншотом.
 */
object NewsFeed {

    /** Верхняя граница карусели: дальше человек не долистывает. */
    const val LIMIT = 12

    fun cards(items: List<DiscoveredNews.Item>, now: Long = System.currentTimeMillis()): List<NewsItem> =
        items.take(LIMIT).mapIndexed { index, item ->
            NewsItem(
                id = item.key,
                category = category(item.host),
                title = item.title,
                // Подзаголовка у ленты нет, и выдумывать аннотацию не из
                // чего: показываем адрес новости, он хотя бы правдив.
                subtitle = path(item.url),
                source = source(item.host),
                age = age(item.date, now),
                url = item.url,
                tone = NewsTone.entries[index % NewsTone.entries.size],
            )
        }

    /**
     * Возраст новости словами.
     *
     * Пустая строка, когда даты нет: «только что» у новости без даты —
     * это утверждение, которого никто не проверял.
     */
    fun age(date: String, now: Long): String {
        val at = parseDate(date) ?: return ""
        val days = ((now - at) / DAY_MS).toInt()
        return when {
            days < 0 -> ""
            days == 0 -> "сегодня"
            days == 1 -> "вчера"
            days < 7 -> "$days дн. назад"
            days < 31 -> "${days / 7} нед. назад"
            else -> "${days / 30} мес. назад"
        }
    }

    /**
     * Дата из ленты приходит как есть: `<time datetime>` даёт ISO,
     * текст карточки — что угодно. Берём начало ISO-строки, потому что
     * именно оно устойчиво; на остальное не гадаем.
     */
    fun parseDate(raw: String): Long? {
        val match = ISO.find(raw.trim()) ?: return null
        val (y, m, d) = match.destructured
        val year = y.toIntOrNull() ?: return null
        val month = m.toIntOrNull() ?: return null
        val day = d.toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(year, month - 1, day)
        return cal.timeInMillis
    }

    /** Ярлык на обложке: источник новости, а не выдуманная рубрика. */
    private fun category(host: String): String = source(host)

    private fun source(host: String): String =
        host.removePrefix("www.").lowercase(Locale.ROOT).ifEmpty { "источник" }

    private fun path(url: String): String =
        url.substringAfter("://", url).substringAfter('/', "").trimEnd('/').ifEmpty { url }

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private val ISO = Regex("""(\d{4})-(\d{2})-(\d{2})""")
}
