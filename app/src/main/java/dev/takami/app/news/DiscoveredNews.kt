package dev.takami.app.news

import android.content.Context
import core.model.MediaItem
import java.io.File

/**
 * Новости, снятые автопарсером с разобранных лент.
 *
 * Отдельно от `DiscoveredTitles` намеренно: у новости другой срок
 * жизни и другой порядок. Тайтл в подборе актуален месяцами, новость
 * устаревает за дни, и смешивать их в одном списке значит либо
 * выбрасывать живые тайтлы вместе с протухшими новостями, либо
 * показывать в ленте прошлогоднее.
 *
 * Формат тот же построчный, что у `DiscoveredTitles`, по той же
 * причине: плагин сериализации ради пяти строковых полей не стоит
 * правки общего build-файла.
 */
class DiscoveredNews(context: Context) {

    private val file = File(context.filesDir, "discovered-news.tsv")

    data class Item(
        val key: String,
        val title: String,
        val url: String,
        val cover: String? = null,
        val date: String = "",
        val host: String = "",
    )

    fun all(): List<Item> {
        if (!file.isFile) return emptyList()
        return runCatching { file.readLines().mapNotNull(::decode) }.getOrDefault(emptyList())
    }

    /** Свежее сверху: лента без порядка по дате бесполезна. */
    fun latest(limit: Int = 20): List<Item> =
        all().sortedByDescending { it.date }.take(limit)

    fun add(items: List<MediaItem>, host: String) {
        if (items.isEmpty()) return
        val existing = all().associateBy { it.key }.toMutableMap()
        items.forEach { item ->
            existing[item.key] = Item(
                key = item.key,
                title = item.title,
                url = item.url,
                cover = item.cover,
                // Дату кладёт холодный старт, когда находит её в
                // карточке; если ленты без дат — сортировка просто
                // сохранит порядок разбора.
                date = item.extras["date"].orEmpty(),
                host = host,
            )
        }
        val trimmed = existing.values.toList().takeLast(MAX_ITEMS)
        runCatching { file.writeText(trimmed.joinToString("\n", transform = ::encode)) }
    }

    fun clear() {
        runCatching { file.delete() }
    }

    private fun encode(item: Item): String = listOf(
        item.key, item.title, item.url, item.cover.orEmpty(), item.date, item.host,
    ).joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') }

    private fun decode(line: String): Item? {
        if (line.isBlank()) return null
        val parts = line.split('\t')
        if (parts.size < 6) return null
        return Item(
            key = parts[0],
            title = parts[1],
            url = parts[2],
            cover = parts[3].takeIf { it.isNotEmpty() },
            date = parts[4],
            host = parts[5],
        )
    }

    private companion object {
        const val MAX_ITEMS = 300
    }
}
