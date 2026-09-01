package com.mangareader.reader.engine.source

import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Offline [MangaPageSource] backed by a plain directory of image files.
 *
 * This exists per the task spec as one of two reference implementations
 * to develop and validate the reader against before any real parser
 * exists — and per §2.1's whole point, it is not throwaway scaffolding:
 * this is *the* offline/downloaded-chapter code path. A future "download
 * chapter" feature in the host app only needs to save pages into a
 * folder-per-chapter layout and point this class at it; the reader
 * requires no separate "offline mode" implementation.
 *
 * Directory layout expected:
 * ```
 * root/
 *   <chapterId>/
 *     0001.jpg
 *     0002.png
 *     ...
 * ```
 * Pages are ordered by filename (natural/lexicographic sort of the
 * numeric-prefixed names). Chapter ordering for [nextChapter]/[prevChapter]
 * is derived from [chapterOrder], which the caller supplies since a plain
 * folder has no other place to encode series order.
 */
class FolderPageSource(
    private val root: File,
    /** Chapter ids in reading order, e.g. ["ch01", "ch02", "ch03"]. */
    private val chapterOrder: List<String>,
) : MangaPageSource {

    override suspend fun pages(chapterId: String): List<PageRef> {
        val chapterDir = File(root, chapterId)
        if (!chapterDir.isDirectory) return emptyList()
        val files = chapterDir.listFiles { f -> f.isFile && isImage(f) }
            ?.sortedBy { it.name }
            ?: emptyList()
        return files.mapIndexed { index, file ->
            PageRef(
                id = "$chapterId/${file.name}",
                index = index,
                uri = file.toURI().toString(),
            )
        }
    }

    override fun open(page: PageRef): Flow<PageLoad> = flow {
        val file = File(java.net.URI(page.uri))
        if (!file.exists()) {
            emit(PageLoad.Error(IllegalStateException("File not found: ${page.uri}")))
            return@flow
        }
        val total = file.length()
        emit(PageLoad.Progress(bytes = total, total = total))
        emit(PageLoad.Done(file))
    }

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

    private fun isImage(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }
}
