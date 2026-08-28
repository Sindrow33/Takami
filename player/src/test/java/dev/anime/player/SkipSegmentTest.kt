package dev.anime.player

import dev.anime.player.skip.FakeSkipProvider
import dev.anime.player.skip.SkipSegment
import dev.anime.player.skip.SkipType
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
