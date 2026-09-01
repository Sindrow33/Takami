package dev.anime.player.ui

import dev.anime.player.ui.PlayerGestures.Zone
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerGesturesTest {

    private val width = 1000f

    @Test
    fun zonesLeaveTheCenterNarrowLikeTadami() {
        assertEquals(Zone.Left, PlayerGestures.zoneAt(100f, width))
        assertEquals(Zone.Left, PlayerGestures.zoneAt(399f, width))
        assertEquals(Zone.Center, PlayerGestures.zoneAt(500f, width))
        assertEquals(Zone.Right, PlayerGestures.zoneAt(601f, width))
        assertEquals(Zone.Right, PlayerGestures.zoneAt(999f, width))
    }

    @Test
    fun zoneIsCenterBeforeLayoutIsMeasured() {
        assertEquals(Zone.Center, PlayerGestures.zoneAt(10f, 0f))
    }

    @Test
    fun repeatedTapsAccumulateInTheSameDirection() {
        var acc = 0L
        acc = PlayerGestures.accumulateSeek(acc, forward = true)
        acc = PlayerGestures.accumulateSeek(acc, forward = true)
        acc = PlayerGestures.accumulateSeek(acc, forward = true)
        assertEquals(30_000L, acc)
    }

    @Test
    fun tapInTheOppositeDirectionRestartsTheCount() {
        assertEquals(-10_000L, PlayerGestures.accumulateSeek(30_000L, forward = false))
    }

    @Test
    fun seekIsClampedToTheEpisode() {
        assertEquals(0L, PlayerGestures.seekTarget(5_000L, -10_000L, 60_000L))
        assertEquals(60_000L, PlayerGestures.seekTarget(55_000L, 10_000L, 60_000L))
        assertEquals(15_000L, PlayerGestures.seekTarget(5_000L, 10_000L, 60_000L))
    }

    @Test
    fun seekIsClampedAtZeroWithUnknownDuration() {
        assertEquals(0L, PlayerGestures.seekTarget(5_000L, -10_000L, 0L))
        assertEquals(15_000L, PlayerGestures.seekTarget(5_000L, 10_000L, 0L))
    }

    @Test
    fun seekLabelShowsSignedSeconds() {
        assertEquals("+30 s", PlayerGestures.seekLabel(30_000L))
        assertEquals("-10 s", PlayerGestures.seekLabel(-10_000L))
    }

    @Test
    fun swipeUpIncreasesAndIsClamped() {
        assertEquals(0.8f, PlayerGestures.verticalValue(0.5f, 500f, 200f, 1000f), 0.001f)
        assertEquals(0.2f, PlayerGestures.verticalValue(0.5f, 500f, 800f, 1000f), 0.001f)
        assertEquals(1f, PlayerGestures.verticalValue(0.9f, 900f, 0f, 1000f), 0.001f)
        assertEquals(0f, PlayerGestures.verticalValue(0.1f, 0f, 900f, 1000f), 0.001f)
    }

    @Test
    fun verticalGestureIsInertBeforeLayout() {
        assertEquals(0.5f, PlayerGestures.verticalValue(0.5f, 0f, 900f, 0f), 0.001f)
    }

    @Test
    fun volumeStepsRoundToDeviceScale() {
        assertEquals(0, PlayerGestures.volumeSteps(0f, 15))
        assertEquals(8, PlayerGestures.volumeSteps(0.5f, 15))
        assertEquals(15, PlayerGestures.volumeSteps(1f, 15))
        assertEquals(15, PlayerGestures.volumeSteps(2f, 15))
    }

    @Test
    fun percentIsRounded() {
        assertEquals(0, PlayerGestures.percent(0f))
        assertEquals(50, PlayerGestures.percent(0.5f))
        assertEquals(100, PlayerGestures.percent(1.4f))
    }

    @Test
    fun horizontalSeekFollowsTheFingerAndClamps() {
        assertEquals(30_000L, PlayerGestures.horizontalSeek(20_000L, 100f, 200f, 600_000L, 100f))
        assertEquals(0L, PlayerGestures.horizontalSeek(5_000L, 500f, 0f, 600_000L, 100f))
        assertEquals(600_000L, PlayerGestures.horizontalSeek(590_000L, 0f, 900f, 600_000L, 100f))
    }

    @Test
    fun speedCyclesThroughPresets() {
        assertEquals(1.25f, PlayerGestures.nextSpeed(1f), 0.001f)
        assertEquals(0.5f, PlayerGestures.nextSpeed(2f), 0.001f)
        assertEquals(0.5f, PlayerGestures.nextSpeed(3f), 0.001f)
    }

    @Test
    fun speedLabelDropsTrailingZero() {
        assertEquals("1x", PlayerGestures.speedLabel(1f))
        assertEquals("1.25x", PlayerGestures.speedLabel(1.25f))
        assertEquals("2x", PlayerGestures.speedLabel(2f))
    }
}
