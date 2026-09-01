package dev.takami.app.swipes

import android.content.Context
import core.model.MediaItem
import java.io.File
/**
 * Тайтлы, которые автопарсер снял с разобранных сайтов.
 *
 * Хранится отдельным файлом, а не в реестре источников: реестру нужны
 * селекторы и здоровье хоста, а здесь — список того, что можно предложить
 * пользователю. Список переживает перезапуск, иначе после каждого запуска
 * подбор снова пустой, пока сайт не разберут заново.
 */
class DiscoveredTitles(context: Context) {

    private val file = File(context.filesDir, "discovered-titles.json")

    data class Entry(
        val key: String,
        val title: String,
        val url: String,
        val cover: String? = null,
        val host: String = "",
    )

    fun all(): List<Entry> {
        if (!file.isFile) return emptyList()
        return runCatching {
            file.readLines().mapNotNull(::decode)
        }.getOrDefault(emptyList())
    }

    /**
     * Добавляет разобранное, не теряя прежнее и не дублируя.
     *
     * Дедупликация по ключу источника: один и тот же тайтл приезжает при
     * каждом разборе каталога, и без неё колода за три разбора состояла бы
     * из трёх копий одного списка.
     */
    fun add(items: List<MediaItem>, host: String) {
        if (items.isEmpty()) return
        val existing = all().associateBy { it.key }.toMutableMap()
        items.forEach { item ->
            existing[item.key] = Entry(
                key = item.key,
                title = item.title,
                url = item.url,
                cover = item.cover,
                host = host,
            )
        }
        val trimmed = existing.values.toList().takeLast(MAX_ENTRIES)
        runCatching { file.writeText(trimmed.joinToString("\n", transform = ::encode)) }
    }

    fun clear() {
        runCatching { file.delete() }
    }

    /*
     * Свой построчный формат вместо JSON: подключать плагин сериализации
     * ради пяти строковых полей значило бы править общий build-файл, а он
     * чужой. Разделитель — табуляция: в названии тайтла и в URL её не бывает.
     */
    private fun encode(entry: Entry): String = listOf(
        entry.key, entry.title, entry.url, entry.cover.orEmpty(), entry.host,
    ).joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') }

    private fun decode(line: String): Entry? {
        if (line.isBlank()) return null
        val parts = line.split('\t')
        if (parts.size < 5) return null
        return Entry(
            key = parts[0],
            title = parts[1],
            url = parts[2],
            cover = parts[3].takeIf { it.isNotEmpty() },
            host = parts[4],
        )
    }

    private companion object {
        /** Больше не нужно: колода на 500 карточек не пролистывается вручную. */
        const val MAX_ENTRIES = 500
    }
}
