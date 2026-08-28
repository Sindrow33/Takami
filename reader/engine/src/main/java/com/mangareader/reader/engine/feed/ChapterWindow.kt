package com.mangareader.reader.engine.feed

import com.mangareader.core.model.ChapterInfo
import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageRef

/**
 * Loads and holds the page list for one chapter, and knows how to reach
 * its neighbours via [MangaPageSource]. This is a thin wrapper — the
 * actual "no chapter boundary" feed behaviour lives in [FeedController],
 * which stitches multiple [ChapterWindow]s into one flat page sequence.
 */
internal class ChapterWindow(
    val chapter: ChapterInfo,
    val pages: List<PageRef>,
)

internal class ChapterWindowLoader(
    private val source: MangaPageSource,
    private val chapterLookup: suspend (String) -> ChapterInfo,
) {
    suspend fun load(chapterId: String): ChapterWindow {
        val info = chapterLookup(chapterId)
        val pages = source.pages(chapterId)
        return ChapterWindow(info, pages)
    }

    suspend fun nextChapterId(currentChapterId: String): String? = source.nextChapter(currentChapterId)
    suspend fun prevChapterId(currentChapterId: String): String? = source.prevChapter(currentChapterId)
}
