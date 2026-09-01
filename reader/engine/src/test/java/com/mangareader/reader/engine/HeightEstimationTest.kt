package com.mangareader.reader.engine

import com.mangareader.core.model.ChapterInfo
import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.feed.FeedController
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.engine.layout.SeamlessLayoutEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Путь БЕЗ `PageRef.width/height` — основной для онлайн-источников:
 * сайты размеров страниц не публикуют, качать их ради размеров смысла
 * нет. Значит running-average оценка это не запасной вариант, а рабочий,
 * и прыжки ленты ловятся именно здесь.
 */
private class SizelessSource(
    private val chapters: List<String>,
    private val pagesPerChapter: Int,
) : MangaPageSource {
    override suspend fun pages(chapterId: String): List<PageRef> =
        (0 until pagesPerChapter).map {
            // Ни width, ни height — ровно то, что отдаст парсер.
            PageRef(id = "$chapterId/$it", index = it, uri = "https://x/$chapterId/$it")
        }

    override fun open(page: PageRef): Flow<PageLoad> = flowOf(PageLoad.Error(UnsupportedOperationException()))
    override suspend fun nextChapter(chapterId: String): String? =
        chapters.getOrNull(chapters.indexOf(chapterId) + 1)
    override suspend fun prevChapter(chapterId: String): String? {
        val i = chapters.indexOf(chapterId)
        return if (i <= 0) null else chapters[i - 1]
    }
}

class HeightEstimationTest {

    private fun controller(chapters: List<String>, pages: Int, scope: TestScope) = FeedController(
        source = SizelessSource(chapters, pages),
        chapterLookup = { id -> ChapterInfo(id = id, mangaId = "m", number = chapters.indexOf(id) + 1f) },
        scope = scope,
    )

    private fun pageIndices(feed: FeedController): List<Int> =
        feed.state.value.items.indices.filter { feed.state.value.items[it] is FeedItem.Page }

    @Test
    fun `оценка идёт от реальной ширины ленты, а не от значения по умолчанию`() =
        runTest(UnconfinedTestDispatcher()) {
            val feed = controller(listOf("ch01"), pages = 40, scope = this)
            feed.start("ch01", 0)
            feed.updateLayoutWidth(1440)

            val page = feed.state.value.items.first { it is FeedItem.Page } as FeedItem.Page
            // 1440 * 1.45 — дефолтное webtoon-отношение при ширине 1440,
            // а не 1080-е значение по умолчанию.
            assertEquals((1440 * 1.45f).toInt(), page.layoutHeightPx)
        }

    @Test
    fun `на длинной главе средняя высота сходится к реальной`() =
        runTest(UnconfinedTestDispatcher()) {
            val feed = controller(listOf("ch01"), pages = 60, scope = this)
            feed.start("ch01", 0)
            feed.updateLayoutWidth(1080)

            // Реальные страницы главы заметно выше дефолтной оценки:
            // отношение 2.2 против 1.45 — типичный вебтун-стрип.
            val realHeight = (1080 * 2.2f).toInt()
            val indices = pageIndices(feed)

            // Прочитали первые 10 страниц — движок их измерил.
            indices.take(10).forEach { feed.reportMeasuredHeight(it, realHeight) }

            // Оценка ещё не прочитанной страницы должна подтянуться к реальной.
            val stillEstimated = feed.state.value.items
                .filterIsInstance<FeedItem.Page>()
                .first { it.isHeightEstimated }
            val drift = abs(stillEstimated.layoutHeightPx - realHeight) / realHeight.toFloat()
            assertTrue(
                drift < 0.1f,
                "после 10 измеренных страниц оценка обязана быть в пределах 10% от реальной, а не $drift",
            )
        }

    @Test
    fun `догрузка следующей главы не сбрасывает уже измеренные высоты`() =
        runTest(UnconfinedTestDispatcher()) {
            val feed = controller(listOf("ch01", "ch02"), pages = 20, scope = this)
            feed.start("ch01", 0)
            feed.updateLayoutWidth(1080)

            val measured = 3333
            val indices = pageIndices(feed)
            indices.take(5).forEach { feed.reportMeasuredHeight(it, measured) }

            // Подъезжаем к концу главы — контроллер догружает вторую и
            // пересобирает плоский список.
            feed.onViewportMoved(feed.state.value.items.lastIndex)
            assertEquals(40, feed.state.value.items.filterIsInstance<FeedItem.Page>().size)

            val firstFive = feed.state.value.items.filterIsInstance<FeedItem.Page>().take(5)
            assertTrue(
                firstFive.all { it.layoutHeightPx == measured && !it.isHeightEstimated },
                "измеренные высоты обязаны пережить пересборку ленты — иначе лента прыгает на границе глав",
            )
        }

    @Test
    fun `измеренные высоты не создают зазора в вёрстке`() =
        runTest(UnconfinedTestDispatcher()) {
            val feed = controller(listOf("ch01"), pages = 30, scope = this)
            feed.start("ch01", 0)
            feed.updateLayoutWidth(1080)

            // Смешанный случай: часть страниц измерена, часть ещё нет.
            pageIndices(feed).filter { it % 2 == 0 }.forEach {
                feed.reportMeasuredHeight(it, 1500 + it * 7)
            }

            val layout = SeamlessLayoutEngine.layout(feed.state.value.items)
            layout.items.zipWithNext { above, below ->
                assertEquals(above.bottom, below.top, "шов не должен разъезжаться на смешанных высотах")
            }
            assertTrue(layout.items.all { it.heightPx > 0 })
        }

    @Test
    fun `смена ширины ленты не оставляет высоты из старой системы координат`() =
        runTest(UnconfinedTestDispatcher()) {
            val feed = controller(listOf("ch01"), pages = 20, scope = this)
            feed.start("ch01", 0)
            feed.updateLayoutWidth(1080)
            pageIndices(feed).take(5).forEach { feed.reportMeasuredHeight(it, 2376) } // aspect 2.2

            // Поворот экрана: ширина выросла.
            feed.updateLayoutWidth(1920)

            val pages = feed.state.value.items.filterIsInstance<FeedItem.Page>()
            assertTrue(
                pages.all { it.isHeightEstimated },
                "после смены ширины старые пиксельные высоты неприменимы и должны стать оценкой",
            )
            // Накопленное отношение (~2.2) обязано пережить поворот.
            val expected = (1920 * 2.2f).toInt()
            val drift = abs(pages.first().layoutHeightPx - expected) / expected.toFloat()
            assertTrue(drift < 0.1f, "отношение сторон должно переживать поворот, дрейф $drift")
        }
}
