package com.mangareader.reader.engine

import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.layout.EndCapKind
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.engine.layout.SeamlessLayoutEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeamlessLayoutEngineTest {

    private fun page(index: Int, height: Int) = FeedItem.Page(
        chapterId = "ch01",
        chapterNumber = 1f,
        pageRef = PageRef(id = "p$index", index = index, uri = "file:///p$index"),
        layoutHeightPx = height,
        isHeightEstimated = false,
    )

    @Test
    fun `соседние страницы стыкуются без зазора и без нахлёста`() {
        val items = (0 until 20).map { page(it, 1487) }
        val result = SeamlessLayoutEngine.layout(items)

        result.items.zipWithNext { above, below ->
            assertEquals(above.bottom, below.top, "шов между страницами не должен иметь зазора")
        }
    }

    @Test
    fun `сумма высот равна общей высоте ленты`() {
        val items = (0 until 50).map { page(it, 999) } +
            FeedItem.EndCap(EndCapKind.SERIES_END_CAUGHT_UP, layoutHeightPx = 640)
        val result = SeamlessLayoutEngine.layout(items)

        assertEquals(result.totalHeightPx, result.items.sumOf { it.heightPx })
        assertEquals(0, result.items.first().top)
        assertEquals(result.totalHeightPx, result.items.last().bottom)
    }

    @Test
    fun `высоты остаются положительными при мелких страницах`() {
        val items = (0 until 10).map { page(it, 3) }
        val result = SeamlessLayoutEngine.layout(items)
        assertTrue(result.items.all { it.heightPx > 0 })
    }
}
