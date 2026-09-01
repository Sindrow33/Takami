package dev.anime.player.host

import dev.anime.player.ui.formatTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPositionTest {

    private val duration = 24 * 60 * 1000L // 24:00

    @Test
    fun startsFromBeginningWhenBarelyWatched() {
        assertEquals(0L, PlaybackPosition.resumeFrom(0L, duration))
        assertEquals(0L, PlaybackPosition.resumeFrom(4_000L, duration))
    }

    @Test
    fun resumesFromSavedPositionInTheMiddle() {
        assertEquals(750_000L, PlaybackPosition.resumeFrom(750_000L, duration))
    }

    @Test
    fun restartsWatchedEpisodeFromZero() {
        assertEquals(0L, PlaybackPosition.resumeFrom(duration - 5_000L, duration))
        assertEquals(0L, PlaybackPosition.resumeFrom(duration, duration))
    }

    @Test
    fun resumesWithUnknownDuration() {
        assertEquals(600_000L, PlaybackPosition.resumeFrom(600_000L, 0L))
    }

    @Test
    fun savesOnlyMeaningfulPositions() {
        assertFalse(PlaybackPosition.shouldSave(2_000L, duration))
        assertTrue(PlaybackPosition.shouldSave(300_000L, duration))
        assertFalse(PlaybackPosition.shouldSave(duration - 1_000L, duration))
    }

    @Test
    fun marksWatchedNearTheEnd() {
        assertTrue(PlaybackPosition.isWatched(duration - 1_000L, duration))
        assertFalse(PlaybackPosition.isWatched(duration / 2, duration))
        assertFalse(PlaybackPosition.isWatched(1_000L, 0L))
    }

    @Test
    fun progressFractionIsClamped() {
        assertEquals(0f, PlaybackPosition.progressFraction(0L, duration), 0.001f)
        assertEquals(0.5f, PlaybackPosition.progressFraction(duration / 2, duration), 0.001f)
        assertEquals(1f, PlaybackPosition.progressFraction(duration * 2, duration), 0.001f)
        assertEquals(0f, PlaybackPosition.progressFraction(1_000L, 0L), 0.001f)
    }

    @Test
    fun labelMatchesWhatTheListShows() {
        assertEquals("", PlaybackPosition.resumeLabel(1_000L, duration, ::formatTime))
        assertEquals(
            "остановились на 12:30 из 24:00",
            PlaybackPosition.resumeLabel(750_000L, duration, ::formatTime),
        )
        assertEquals("просмотрено", PlaybackPosition.resumeLabel(duration, duration, ::formatTime))
    }
}
