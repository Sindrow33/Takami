package dev.anime.player

import dev.anime.player.skip.FakeSkipProvider
import dev.anime.player.skip.SkipSegment
import dev.anime.player.skip.SkipType
import dev.anime.player.skip.activeSegmentAt
import dev.anime.player.ui.formatTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipSegmentTest {

    @Test
    fun containsIsInclusiveStartExclusiveEnd() {
        val seg = SkipSegment(SkipType.OP, 1000, 2000)
        assertTrue(seg.contains(1000))
        assertTrue(seg.contains(1999))
        assertFalse(seg.contains(2000))
        assertFalse(seg.contains(999))
    }

    @Test
    fun fakeProviderOmitsEdForShortVideos() = runTest {
        assertEquals(1, FakeSkipProvider().segments(null, 1, 60000).size)
        assertEquals(2, FakeSkipProvider().segments(null, 1, 1440000).size)
    }

    @Test
    fun timeFormatting() {
        assertEquals("0:00", formatTime(0))
        assertEquals("1:05", formatTime(65000))
        assertEquals("1:00:00", formatTime(3600000))
    }
}

class ActiveSegmentTest {

    private val recap = SkipSegment(SkipType.RECAP, 0L, 30_000L)
    private val op = SkipSegment(SkipType.OP, 12_500L, 102_000L)
    private val segments = listOf(recap, op)

    @org.junit.Test
    fun picksFurthestEndAmongOverlappingSegments() {
        // 0:20 попадает и в рекап, и в опенинг — предложить надо тот, что уводит дальше.
        org.junit.Assert.assertEquals(op, activeSegmentAt(segments, 20_000L))
    }

    @org.junit.Test
    fun picksTheOnlyMatchOutsideOverlap() {
        org.junit.Assert.assertEquals(recap, activeSegmentAt(segments, 1_000L))
        org.junit.Assert.assertEquals(op, activeSegmentAt(segments, 90_000L))
    }

    @org.junit.Test
    fun returnsNullOutsideAnySegment() {
        org.junit.Assert.assertNull(activeSegmentAt(segments, 200_000L))
        org.junit.Assert.assertNull(activeSegmentAt(emptyList(), 0L))
    }
}
