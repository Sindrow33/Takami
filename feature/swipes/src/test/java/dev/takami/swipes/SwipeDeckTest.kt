package dev.takami.swipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeMathTest {

    private val width = 1000f

    @Test
    fun commitsOnlyPastTheThreshold() {
        assertEquals(SwipeDirection.None, SwipeMath.directionFor(120f, width))
        assertEquals(SwipeDirection.Like, SwipeMath.directionFor(300f, width))
        assertEquals(SwipeDirection.Skip, SwipeMath.directionFor(-300f, width))
    }

    @Test
    fun neverCommitsWithUnknownWidth() {
        assertEquals(SwipeDirection.None, SwipeMath.directionFor(900f, 0f))
        assertEquals(0f, SwipeMath.dragProgress(900f, 0f), 0.001f)
    }

    @Test
    fun dragProgressIsClampedBothWays() {
        assertEquals(1f, SwipeMath.dragProgress(900f, width), 0.001f)
        assertEquals(-1f, SwipeMath.dragProgress(-900f, width), 0.001f)
        assertEquals(0.5f, SwipeMath.dragProgress(140f, width), 0.01f)
    }

    @Test
    fun tiltStaysWithinLimit() {
        assertTrue(SwipeMath.tiltDegrees(5000f, width) <= SwipeMath.MAX_TILT_DEG)
        assertTrue(SwipeMath.tiltDegrees(-5000f, width) >= -SwipeMath.MAX_TILT_DEG)
        assertEquals(0f, SwipeMath.tiltDegrees(0f, width), 0.001f)
    }

    @Test
    fun badgeStaysHiddenOnTinyDrag() {
        assertEquals(0f, SwipeMath.badgeAlpha(10f, width), 0.001f)
        assertEquals(1f, SwipeMath.badgeAlpha(400f, width), 0.001f)
    }

    @Test
    fun underCardGrowsWithDrag() {
        assertEquals(0.94f, SwipeMath.underCardScale(0f, width), 0.001f)
        assertEquals(1f, SwipeMath.underCardScale(400f, width), 0.001f)
    }

    @Test
    fun flyAwayGoesOffScreenBothWays() {
        assertTrue(SwipeMath.flyAwayX(SwipeDirection.Like, width) > width)
        assertTrue(SwipeMath.flyAwayX(SwipeDirection.Skip, width) < -width)
        assertEquals(0f, SwipeMath.flyAwayX(SwipeDirection.None, width), 0.001f)
    }
}

class DeckStateTest {

    private val deck = DeckState(
        listOf(
            DeckCard("d1", "One", "", "Аниме"),
            DeckCard("d2", "Two", "", "Аниме"),
            DeckCard("d3", "Three", "", "Аниме"),
            DeckCard("d4", "Four", "", "Манга"),
            DeckCard("d5", "Five", "", "Манга"),
        )
    )

    @Test
    fun advancesAndRecordsChoices() {
        val after = deck.apply(SwipeDirection.Like).apply(SwipeDirection.Skip)
        assertEquals(2, after.index)
        assertEquals(listOf("d1"), after.liked)
        assertEquals(listOf("d2"), after.skipped)
        assertEquals("d3", after.current?.id)
        assertEquals("d4", after.next?.id)
    }

    @Test
    fun ignoresIncompleteSwipe() {
        assertEquals(deck, deck.apply(SwipeDirection.None))
    }

    @Test
    fun finishesAfterLastCardAndDoesNotOverrun() {
        var s = deck
        repeat(deck.cards.size + 3) { s = s.apply(SwipeDirection.Like) }
        assertTrue(s.isFinished)
        assertEquals(deck.cards.size, s.index)
        assertEquals(deck.cards.size, s.liked.size)
        assertEquals(0, s.remaining)
        assertNull(s.current)
    }

    @Test
    fun restartClearsChoices() {
        val restarted = deck.apply(SwipeDirection.Like).restart()
        assertEquals(0, restarted.index)
        assertTrue(restarted.liked.isEmpty())
        assertFalse(restarted.isFinished)
    }

    @Test
    fun emptyDeckIsFinishedImmediately() {
        val empty = DeckState(emptyList())
        assertTrue(empty.isFinished)
        assertNull(empty.current)
    }
}

class CoverSampleSizeTest {

    @Test
    fun keepsFullSizeWhenSourceIsSmall() {
        assertEquals(1, CoverLoader.sampleSizeFor(400, 500))
        assertEquals(1, CoverLoader.sampleSizeFor(500, 500))
    }

    @Test
    fun halvesUntilItFits() {
        assertEquals(2, CoverLoader.sampleSizeFor(1000, 500))
        assertEquals(4, CoverLoader.sampleSizeFor(2000, 500))
        assertEquals(8, CoverLoader.sampleSizeFor(4000, 500))
    }

    @Test
    fun neverGoesBelowOneOnGarbageInput() {
        assertEquals(1, CoverLoader.sampleSizeFor(0, 500))
        assertEquals(1, CoverLoader.sampleSizeFor(1000, 0))
        assertEquals(1, CoverLoader.sampleSizeFor(-10, -10))
    }
}

class FilterUndecidedTest {

    private val cards = listOf(
        DeckCard("a", "A", "", "Аниме"),
        DeckCard("b", "B", "", "Аниме"),
        DeckCard("c", "C", "", "Манга"),
    )

    @Test
    fun removesAlreadyDecidedCards() {
        val left = SwipeMath.filterUndecided(cards, setOf("a", "c"))
        assertEquals(listOf("b"), left.map { it.id })
    }

    @Test
    fun emptyDecisionsKeepEverything() {
        assertEquals(3, SwipeMath.filterUndecided(cards, emptySet()).size)
    }

    @Test
    fun allDecidedGivesEmptyDeck() {
        assertTrue(SwipeMath.filterUndecided(cards, setOf("a", "b", "c")).isEmpty())
    }

    @Test
    fun unknownIdsAreIgnored() {
        assertEquals(3, SwipeMath.filterUndecided(cards, setOf("zzz")).size)
    }
}
