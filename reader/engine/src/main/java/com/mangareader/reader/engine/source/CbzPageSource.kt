package com.mangareader.reader.engine.source

import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.lingala.zip4j.ZipFile
import java.io.File

/**
 * Offline [MangaPageSource] backed by one CBZ/ZIP archive per chapter —
 * the other reference/offline implementation required by the task spec.
 *
 * Layout expected: [chapterArchives] maps chapterId -> a `.cbz`/`.zip`
 * file whose entries are page images, named so that lexicographic order
 * is reading order (e.g. `001.jpg`, `002.jpg`, ...). Non-image entries
 * (ComicInfo.xml, thumbs) are ignored.
 *
 * Pages are extracted lazily and cached under [extractCacheDir] keyed by
 * `<chapterId>/<entryName>`, so re-opening an already-read chapter is a
 * cheap filesystem check rather than a re-extraction.
 */
class CbzPageSource(
    private val chapterArchives: LinkedHashMap<String, File>, // insertion order = reading order
    private val extractCacheDir: File,
) : MangaPageSource {

    private val chapterOrder: List<String> = chapterArchives.keys.toList()

    override suspend fun pages(chapterId: String): List<PageRef> {
        val archive = chapterArchives[chapterId] ?: return emptyList()
        ZipFile(archive).use { zip ->
            val entries = zip.fileHeaders
                .filter { !it.isDirectory && isImage(it.fileName) }
                .sortedBy { it.fileName }
            return entries.mapIndexed { index, header ->
                PageRef(
                    id = "$chapterId/${header.fileName}",
                    index = index,
                    uri = "cbz://$chapterId/${header.fileName}",
                )
            }
        }
    }

    override fun open(page: PageRef): Flow<PageLoad> = flow {
        val (chapterId, entryName) = parseUri(page.uri)
        val archive = chapterArchives[chapterId]
        if (archive == null) {
            emit(PageLoad.Error(IllegalStateException("Unknown chapter archive: $chapterId")))
            return@flow
        }
        val destDir = File(extractCacheDir, chapterId).apply { mkdirs() }
        val destFile = File(destDir, sanitize(entryName))

        if (destFile.exists() && destFile.length() > 0) {
            emit(PageLoad.Progress(destFile.length(), destFile.length()))
            emit(PageLoad.Done(destFile))
            return@flow
        }

        val zip = ZipFile(archive)
        val header = zip.getFileHeader(entryName)
        if (header == null) {
            emit(PageLoad.Error(IllegalStateException("Entry not found in archive: $entryName")))
            return@flow
        }
        val total = header.uncompressedSize
        emit(PageLoad.Progress(0, total))
        zip.extractFile(header, destDir.absolutePath, destFile.name)
        emit(PageLoad.Progress(total, total))
        emit(PageLoad.Done(destFile))
    }.flowOn(Dispatchers.IO)

    override suspend fun nextChapter(chapterId: String): String? {
        val idx = chapterOrder.indexOf(chapterId)
        if (idx < 0 || idx + 1 >= chapterOrder.size) return null
        return chapterOrder[idx + 1]
    }

    override suspend fun prevChapter(chapterId: String): String? {
        val idx = chapterOrder.indexOf(chapterId)
        if (idx <= 0) return null
        return chapterOrder[idx - 1]
    }

    private fun parseUri(uri: String): Pair<String, String> {
        val stripped = uri.removePrefix("cbz://")
        val chapterId = stripped.substringBefore("/")
        val entryName = stripped.substringAfter("/")
        return chapterId to entryName
    }

    private fun sanitize(entryName: String): String = entryName.replace("/", "_")

    private fun isImage(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }
}
