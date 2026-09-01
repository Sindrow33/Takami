package com.mangareader.reader.engine

import com.mangareader.core.model.ChapterInfo
import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.feed.FeedController
import com.mangareader.reader.engine.layout.EndCapKind
import com.mangareader.reader.engine.layout.FeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeSource(private val chapters: List<String>, private val pagesPerChapter: Int) : MangaPageSource {
    override suspend fun pages(chapterId: String): List<PageRef> =
        (0 until pagesPerChapter).map {
            PageRef(id = "$chapterId/$it", index = it, uri = "fake://$chapterId/$it", width = 1000, height = 1500)
        }

    override fun open(page: PageRef): Flow<PageLoad> = flowOf(PageLoad.Error(UnsupportedOperationException()))

    override suspend fun nextChapter(chapterId: String): String? =
        chapters.getOrNull(chapters.indexOf(chapterId) + 1)

    override suspend fun prevChapter(chapterId: String): String? {
        val i = chapters.indexOf(chapterId)
        return if (i <= 0) null else chapters[i - 1]
    }
}

class FeedControllerTest {

    private fun controller(chapters: List<String>, pages: Int, scope: TestScope) = FeedController(
        source = FakeSource(chapters, pages),
        chapterLookup = { id -> ChapterInfo(id = id, mangaId = "m", number = chapters.indexOf(id) + 1f) },
        scope = scope,
    )

    @Test
    fun `первая глава серии получает верхнюю заглушку, а не пустоту`() = runTest(UnconfinedTestDispatcher()) {
        val feed = controller(listOf("ch01", "ch02"), pages = 4, scope = this)
        feed.start("ch01", startPage = 0)

        val items = feed.state.value.items
        assertTrue(items.first() is FeedItem.EndCap)
        assertEquals(EndCapKind.SERIES_START, (items.first() as FeedItem.EndCap).kind)
        assertTrue(items.none { it is FeedItem.EndCap && it.kind == EndCapKind.SERIES_END_CAUGHT_UP })
    }

    @Test
    fun `между главами нет разделяющего элемента`() = runTest(UnconfinedTestDispatcher()) {
        val feed = controller(listOf("ch01", "ch02"), pages = 4, scope = this)
        feed.start("ch01", startPage = 0)
        // Подъезжаем к концу первой главы — вторая должна дозагрузиться.
        feed.onViewportMoved(feed.state.value.items.lastIndex)

        val items = feed.state.value.items
        val pageItems = items.filterIsInstance<FeedItem.Page>()
        assertEquals(8, pageItems.size)
        val boundary = pageItems.zipWithNext().indexOfFirst { (a, b) -> a.chapterId != b.chapterId }
        assertTrue(boundary >= 0, "вторая глава должна быть в той же ленте")
        // Никаких EndCap внутри ленты — только на настоящих краях серии.
        val inner = items.drop(1).dropLast(1)
        assertTrue(inner.none { it is FeedItem.EndCap })
    }

    @Test
    fun `измеренная высота вытесняет оценку`() = runTest(UnconfinedTestDispatcher()) {
        val feed = controller(listOf("ch01"), pages = 3, scope = this)
        feed.start("ch01", startPage = 0)
        val index = feed.state.value.items.indexOfFirst { it is FeedItem.Page }

        feed.reportMeasuredHeight(index, 2222)
        val page = feed.state.value.items[index] as FeedItem.Page
        assertEquals(2222, page.layoutHeightPx)
        assertTrue(!page.isHeightEstimated)
    }
}
