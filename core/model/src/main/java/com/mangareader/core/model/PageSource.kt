package com.mangareader.core.model

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * The one boundary between the reader and "the outside world".
 *
 * The reader module knows *nothing* about websites, HTML, CDNs, mirrors or
 * archive formats. It only ever talks to this interface. A future
 * auto-parser module implements it for online sources; this module ships
 * two implementations of its own ([com.mangareader.reader.engine.source.FolderPageSource]
 * and [com.mangareader.reader.engine.source.CbzPageSource]) that double as
 * the offline/downloaded-chapter code path — same interface, so "online"
 * and "offline" are literally the same reader code.
 *
 * Implementations MUST be safe to call from background/prefetch coroutines
 * and should not assume UI thread or Activity lifecycle.
 */
interface MangaPageSource {

    /** Ordered list of pages for [chapterId]. Index 0 is the first page. */
    suspend fun pages(chapterId: String): List<PageRef>

    /**
     * Starts loading [page]. Emits progress until a terminal [PageLoad.Done]
     * or [PageLoad.Error]. The returned [File] in [PageLoad.Done] MUST point
     * to a stable, already-fully-written local file (cache or original) —
     * the engine will decode it directly, possibly more than once.
     */
    fun open(page: PageRef): Flow<PageLoad>

    /** Id of the chapter that follows [chapterId], or null if it is the latest known. */
    suspend fun nextChapter(chapterId: String): String?

    /** Id of the chapter that precedes [chapterId], or null if it is the first known. */
    suspend fun prevChapter(chapterId: String): String?
}

/**
 * Reference to a single page, as handed to the reader by a [MangaPageSource].
 *
 * The reader never inspects [uri] beyond passing it back to [MangaPageSource.open];
 * it may be `https://...`, `file://...`, or an opaque source-defined scheme.
 * [headers] (Referer/Cookie/auth) are entirely the source's concern — the
 * reader never sets or reads them itself.
 */
data class PageRef(
    val id: String,
    val index: Int,
    val uri: String,
    val headers: Map<String, String> = emptyMap(),
    /**
     * Optional known dimensions. When the source can provide these (e.g.
     * from a manifest, EXIF, or a CBZ central directory) the seamless
     * webtoon layout (§5.6d) can reserve exact space before the bitmap is
     * decoded, eliminating layout jump entirely. When null, the engine
     * falls back to an estimated placeholder height that is corrected once
     * decoding completes.
     */
    val width: Int? = null,
    val height: Int? = null,
)

/** Emitted by [MangaPageSource.open] while a page's bytes are being fetched. */
sealed interface PageLoad {
    data class Progress(val bytes: Long, val total: Long?) : PageLoad
    data class Done(val file: File) : PageLoad
    data class Error(val cause: Throwable) : PageLoad
}
