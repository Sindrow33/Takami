package com.mangareader.reader.engine

import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.layout.EndCapKind
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.engine.layout.PagedNavigator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PagedNavigatorTest {

    private fun page(chapter: String, index: Int, number: Float = 1f) = FeedItem.Page(
        chapterId = chapter,
        chapterNumber = number,
        pageRef = PageRef(id = "$chapter/$index", index = index, uri = "file:///$chapter/$index.jpg"),
        layoutHeightPx = 1000,
        isHeightEstimated = false,
    )

    /** Две главы подряд плюс торец — та же раскладка, что даёт лента. */
    private val feed = listOf(
        page("ch1", 0), page("ch1", 1), page("ch1", 2),
        page("ch2", 0, 2f), page("ch2", 1, 2f),
        FeedItem.EndCap(EndCapKind.SERIES_END_CAUGHT_UP, layoutHeightPx = 400),
    )

    @Test
    fun `листание вперёд идёт по ленте и переходит в следующую главу`() {
        assertEquals(3, PagedNavigator.step(feed, currentIndex = 2, forward = true))
        assertTrue(
            PagedNavigator.crossesChapter(feed, 2, 3),
            "последняя страница главы обязана вести в следующую, а не упираться",
        )
    }

    @Test
    fun `торец ленты — остановка, а не пропуск`() {
        assertEquals(
            5, PagedNavigator.step(feed, currentIndex = 4, forward = true),
            "за последней страницей стоит экран «вы всё прочитали», его нельзя проскакивать",
        )
        assertNull(PagedNavigator.step(feed, currentIndex = 5, forward = true))
    }

    @Test
    fun `назад с первой страницы некуда`() {
        assertNull(PagedNavigator.step(feed, currentIndex = 0, forward = false))
    }

    @Test
    fun `позиция вне остановок не блокирует листание`() {
        // Вьюпорт может ещё не устояться; отказ листать выглядел бы как
        // зависшая читалка.
        assertEquals(
            0, PagedNavigator.step(feed, currentIndex = -1, forward = true),
            "неизвестная позиция должна приводить к ближайшей допустимой",
        )
    }

    @Test
    fun `номер страницы считается внутри своей главы`() {
        assertEquals(0 to 2, PagedNavigator.pageInChapter(feed, 3))
        assertEquals(
            2 to 3, PagedNavigator.pageInChapter(feed, 2),
            "подпись «3 / 3» обязана относиться к главе, а не ко всей ленте",
        )
        assertNull(PagedNavigator.pageInChapter(feed, 5), "у торца нет номера страницы")
    }

    @Test
    fun `граница главы не считается пересечённой внутри одной главы`() {
        assertFalse(PagedNavigator.crossesChapter(feed, 0, 1))
        assertFalse(
            PagedNavigator.crossesChapter(feed, 4, 5),
            "переход на торец — не смена главы, отклик там не нужен",
        )
    }

    @Test
    fun `индекс страницы главы находится по номеру`() {
        assertEquals(4, PagedNavigator.indexOfPage(feed, "ch2", 1))
        assertNull(PagedNavigator.indexOfPage(feed, "ch2", 9))
    }

    @Test
    fun `длинное медленное движение листает`() {
        assertTrue(
            PagedNavigator.shouldFlip(dragFraction = -0.4f, velocityPxPerSec = 50f, viewportWidthPx = 1080),
            "осознанный длинный перелист не должен требовать ещё и скорости",
        )
    }

    @Test
    fun `быстрый короткий рывок листает, а дрожание пальца — нет`() {
        assertTrue(
            PagedNavigator.shouldFlip(dragFraction = -0.1f, velocityPxPerSec = 2000f, viewportWidthPx = 1080),
        )
        assertFalse(
            PagedNavigator.shouldFlip(dragFraction = -0.01f, velocityPxPerSec = 3000f, viewportWidthPx = 1080),
            "тап дрожащим пальцем даёт большую скорость при нулевом сдвиге — это не перелист",
        )
    }

    @Test
    fun `короткое движение возвращает страницу на место`() {
        assertFalse(
            PagedNavigator.shouldFlip(dragFraction = -0.15f, velocityPxPerSec = 100f, viewportWidthPx = 1080),
        )
    }

    @Test
    fun `нулевая ширина экрана не листает`() {
        assertFalse(
            PagedNavigator.shouldFlip(dragFraction = -1f, velocityPxPerSec = 5000f, viewportWidthPx = 0),
            "до первого layout листать нечего, и делить на ноль тоже нельзя",
        )
    }
}
