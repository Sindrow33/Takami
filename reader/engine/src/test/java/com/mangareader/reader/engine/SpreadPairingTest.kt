package com.mangareader.reader.engine

import com.mangareader.reader.engine.layout.SpreadPairing
import com.mangareader.reader.engine.layout.SpreadPairing.Slot
import com.mangareader.reader.engine.layout.SpreadPairing.Spread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpreadPairingTest {

    private fun page(chapter: String = "ch1", wide: Boolean = false) =
        Slot(isPage = true, isWide = wide, chapterId = chapter)

    private val endCap = Slot(isPage = false, isWide = false, chapterId = null)

    @Test
    fun `обычные страницы клеятся по две`() {
        val spreads = SpreadPairing.pair(List(4) { page() })
        assertEquals(listOf(Spread(0, 1), Spread(2, 3)), spreads)
    }

    @Test
    fun `широкая страница занимает экран целиком`() {
        val spreads = SpreadPairing.pair(listOf(page(), page(wide = true), page()))
        assertEquals(
            listOf(Spread(0, null), Spread(1, null), Spread(2, null)),
            spreads,
            "разворот художника, ужатый вдвое рядом с обычной страницей, — это то самое, ради чего режим и включают",
        )
    }

    @Test
    fun `после широкой страницы чётность начинается заново`() {
        // Без сброса пары уехали бы на страницу и весь остаток главы
        // склеивался бы со сдвигом.
        val spreads = SpreadPairing.pair(
            listOf(page(), page(wide = true), page(), page(), page(), page()),
        )
        assertEquals(
            listOf(Spread(0, null), Spread(1, null), Spread(2, 3), Spread(4, 5)),
            spreads,
        )
    }

    @Test
    fun `пара не пересекает границу главы`() {
        val spreads = SpreadPairing.pair(listOf(page("ch1"), page("ch2"), page("ch2")))
        assertEquals(
            listOf(Spread(0, null), Spread(1, 2)),
            spreads,
            "последняя страница главы и первая следующей — две разные книги на одном экране",
        )
    }

    @Test
    fun `торец ленты остаётся своим экраном`() {
        val spreads = SpreadPairing.pair(listOf(page(), page(), endCap))
        assertEquals(listOf(Spread(0, 1), Spread(2, null)), spreads)
    }

    @Test
    fun `нечётный хвост главы не теряется`() {
        val spreads = SpreadPairing.pair(List(3) { page() })
        assertEquals(listOf(Spread(0, 1), Spread(2, null)), spreads)
        var pagesShown = 0
        for (spread in spreads) pagesShown += if (spread.second == null) 1 else 2
        assertEquals(3, pagesShown, "ни одна страница не должна пропасть при разбиении")
    }

    @Test
    fun `разворот действует только в альбомной ориентации`() {
        assertTrue(SpreadPairing.applies(enabled = true, viewportWidthPx = 2000, viewportHeightPx = 1000))
        assertFalse(
            SpreadPairing.applies(enabled = true, viewportWidthPx = 1080, viewportHeightPx = 2400),
            "в портрете две страницы рядом дают полосу вдвое уже экрана — текст нечитаем",
        )
        assertFalse(SpreadPairing.applies(enabled = false, viewportWidthPx = 2000, viewportHeightPx = 1000))
    }

    @Test
    fun `квадратный экран не считается альбомным`() {
        assertFalse(
            SpreadPairing.applies(enabled = true, viewportWidthPx = 1000, viewportHeightPx = 1000),
            "на границе выбираем одностраничный режим — он корректен всегда",
        )
    }

    @Test
    fun `номер разворота находится по любой из двух страниц`() {
        val spreads = SpreadPairing.pair(List(4) { page() })
        assertEquals(0, SpreadPairing.spreadOf(spreads, 1))
        assertEquals(1, SpreadPairing.spreadOf(spreads, 2))
        assertNull(SpreadPairing.spreadOf(spreads, 9))
    }

    @Test
    fun `пустая лента не ломает разбиение`() {
        assertEquals(emptyList(), SpreadPairing.pair(emptyList()))
    }
}
