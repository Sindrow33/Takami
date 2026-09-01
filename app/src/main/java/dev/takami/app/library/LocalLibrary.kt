package dev.takami.app.library

import android.content.Context
import com.mangareader.core.model.ChapterInfo
import com.mangareader.core.model.MangaPageSource
import com.mangareader.reader.engine.source.CbzPageSource
import com.mangareader.reader.engine.source.FolderPageSource
import java.io.File

/**
 * Локальная библиотека: то, что лежит на устройстве, без сети.
 *
 * Раскладка, которую сканируем:
 * ```
 * <files>/manga/<Название>/<Глава>/0001.jpg     — папки со страницами
 * <files>/manga/<Название>/<Глава>.cbz          — архивы
 * ```
 * Онлайн-источники приедут отдельным `MangaPageSource` от парсера и
 * встанут в тот же реестр — читалка разницы не увидит.
 */
object LocalLibrary {

    const val SOURCE_ID_LOCAL = "local"

    data class Title(
        val id: String,
        val name: String,
        val dir: File,
        val chapters: List<Chapter>,
    ) {
        val chapterCount: Int get() = chapters.size
    }

    data class Chapter(
        val id: String,
        val name: String,
        val number: Float,
        val isArchive: Boolean,
    )

    fun rootDir(context: Context): File = File(context.filesDir, "manga").apply { mkdirs() }

    fun titles(context: Context): List<Title> {
        val root = rootDir(context)
        val dirs = root.listFiles { f -> f.isDirectory }?.sortedBy { it.name } ?: return emptyList()
        return dirs.map { dir ->
            Title(id = dir.name, name = dir.name, dir = dir, chapters = chapters(dir))
        }
    }

    private fun chapters(titleDir: File): List<Chapter> {
        val entries = titleDir.listFiles() ?: return emptyList()
        return entries
            .filter { it.isDirectory || it.extension.lowercase() in setOf("cbz", "zip") }
            .sortedBy { it.name }
            .mapIndexed { index, file ->
                val name = if (file.isDirectory) file.name else file.nameWithoutExtension
                Chapter(
                    id = file.name,
                    name = name,
                    number = numberOf(name) ?: (index + 1).toFloat(),
                    isArchive = !file.isDirectory,
                )
            }
    }

    private fun numberOf(name: String): Float? =
        Regex("""\d+(?:[.,]\d+)?""").find(name)?.value?.replace(',', '.')?.toFloatOrNull()

    /** Источник для одного тайтла: папки и архивы в одном списке глав. */
    fun sourceFor(context: Context, title: Title): MangaPageSource {
        val archives = LinkedHashMap<String, File>()
        title.chapters.filter { it.isArchive }.forEach { archives[it.id] = File(title.dir, it.id) }
        return if (archives.size == title.chapters.size && archives.isNotEmpty()) {
            CbzPageSource(archives, File(context.cacheDir, "cbz/${title.id}"))
        } else {
            FolderPageSource(title.dir, title.chapters.map { it.id })
        }
    }

    fun chapterLookup(title: Title): suspend (String) -> ChapterInfo = { chapterId ->
        val chapter = title.chapters.firstOrNull { it.id == chapterId }
        ChapterInfo(
            id = chapterId,
            mangaId = title.id,
            number = chapter?.number ?: 0f,
            title = chapter?.name,
        )
    }
}
