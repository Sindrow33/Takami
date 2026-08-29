package dev.anime.player.dub

import dev.anime.player.core.PlaybackState
import dev.anime.player.core.PlayerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakePlayerEngine : PlayerEngine {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state
    var lastVolume: Float = 1f

    override fun load(url: String, headers: Map<String, String>, startMs: Long) {}
    override fun play() {}
    override fun pause() {}
    override fun seekTo(ms: Long) {}
    override fun setSpeed(speed: Float) {}
    override fun addSubtitleTrack(url: String, language: String, mimeType: String) {}
    override fun setVolume(volume: Float) { lastVolume = volume }
    override fun release() {}
}

class DubDuckingControllerTest {

    @Test
    fun staysAtFullVolumeWhenDubIsNotActive() {
        val engine = FakePlayerEngine()
        val controller = DubDuckingController(engine, duckedVolume = 0.2f, fadeMs = 0L)

        controller.onTick(isDubActive = false, nowMs = 0L)

        assertEquals(1f, engine.lastVolume, 0.001f)
    }

    @Test
    fun dropsToDuckedVolumeInstantlyWhenFadeDisabled() {
        val engine = FakePlayerEngine()
        val controller = DubDuckingController(engine, duckedVolume = 0.2f, fadeMs = 0L)

        controller.onTick(isDubActive = true, nowMs = 0L)

        assertEquals(0.2f, engine.lastVolume, 0.001f)
    }

    @Test
    fun fadesGraduallyTowardsDuckedVolume() {
        val engine = FakePlayerEngine()
        val controller = DubDuckingController(engine, duckedVolume = 0.0f, fadeMs = 1000L)

        controller.onTick(isDubActive = true, nowMs = 0L)
        val atStart = engine.lastVolume
        controller.onTick(isDubActive = true, nowMs = 500L)
        val atMiddle = engine.lastVolume
        controller.onTick(isDubActive = true, nowMs = 1000L)
        val atEnd = engine.lastVolume

        assertTrue("волюм должен монотонно убывать: $atStart -> $atMiddle -> $atEnd", atStart >= atMiddle)
        assertTrue(atMiddle >= atEnd)
        assertEquals(0f, atEnd, 0.01f)
    }

    @Test
    fun returnsToFullVolumeAfterDubEnds() {
        val engine = FakePlayerEngine()
        val controller = DubDuckingController(engine, duckedVolume = 0.2f, fadeMs = 0L)

        controller.onTick(isDubActive = true, nowMs = 0L)
        controller.onTick(isDubActive = false, nowMs = 100L)

        assertEquals(1f, engine.lastVolume, 0.001f)
    }
}
