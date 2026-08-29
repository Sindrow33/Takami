package dev.anime.player.enhance

import dev.anime.player.asr.AsrCoordinator
import dev.anime.player.asr.AsrDiskCache
import dev.anime.player.asr.AsrEngine
import dev.anime.player.asr.AsrBackend
import dev.anime.player.asr.AsrSegment
import dev.anime.player.asr.AudioSource
import dev.anime.player.core.PlaybackState
import dev.anime.player.core.PlayerEngine
import dev.anime.player.dub.DubCoordinator
import dev.anime.player.dub.DubDiskCache
import dev.anime.player.dub.DubDuckingController
import dev.anime.player.dub.DubLine
import dev.anime.player.dub.RoundRobinVoiceMapper
import dev.anime.player.dub.SynthesizedAudioPlayer
import dev.anime.player.dub.SynthesizedClip
import dev.anime.player.dub.TtsProvider
import dev.anime.player.dub.TtsVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private class FakePlayerEngine : PlayerEngine {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state
    var lastVolume = 1f
    override fun load(url: String, headers: Map<String, String>, startMs: Long) {}
    override fun play() {}
    override fun pause() {}
    override fun seekTo(ms: Long) {}
    override fun setSpeed(speed: Float) {}
    override fun addSubtitleTrack(url: String, language: String, mimeType: String) {}
    override fun setVolume(volume: Float) { lastVolume = volume }
    override fun release() {}
}

private class FakeAudioSource : AudioSource {
    override suspend fun pcm16(startMs: Long, endMs: Long, sampleRateHz: Int) = ShortArray(1)
}

private class FakeAsrEngine : AsrEngine {
    override val backend = AsrBackend.WhisperCpp
    override val fingerprint = "fake-asr"
    override val sampleRateHz = 16000
    override suspend fun transcribe(pcm16: ShortArray, sampleRateHz: Int, offsetMs: Long, language: String?) =
        listOf(AsrSegment(0L, 1000L, "фраза"))
}

private class FakeTtsProvider : TtsProvider {
    override val id = "fake"
    override val fingerprint = "fake-tts"
    var calls = 0
    override suspend fun synthesize(text: String, voice: TtsVoice): TtsProvider.SynthesisOutput {
        calls++
        return TtsProvider.SynthesisOutput(text.toByteArray(), "audio/wav", 900L)
    }
}

private class FakeAudioPlayer : SynthesizedAudioPlayer {
    var playedClip: SynthesizedClip? = null
    var stopCalls = 0
    override var isPlaying = false
        private set

    override fun play(clip: SynthesizedClip, playbackSpeed: Float) {
        playedClip = clip
        isPlaying = true
    }

    override fun stop() {
        stopCalls++
        isPlaying = false
    }
}

class PlaybackEnhancerTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    @Test
    fun onTickAdvancesAsrAndExposesGeneratedDocument() = runTest {
        val engine = FakePlayerEngine()
        val asr = AsrCoordinator(
            FakeAsrEngine(),
            FakeAudioSource(),
            AsrDiskCache(tmp.newFolder("asr")),
            windowMs = 20_000L,
            prefetchAheadMs = 5_000L,
        )
        val enhancer = PlaybackEnhancer(engine, asr = asr)
        enhancer.startEpisode("video-1", 60_000L, "ja")

        val state = enhancer.onTick(positionMs = 0L, isPlaying = true, nowMs = 0L)

        assertTrue(state.asrDocument!!.cues.isNotEmpty())
        assertEquals(20_000L, state.asrProgress!!.generatedUpToMs)
    }

    @Test
    fun onTickStartsDubPlaybackWhenPositionEntersALine() = runTest {
        val engine = FakePlayerEngine()
        val provider = FakeTtsProvider()
        val dub = DubCoordinator(
            provider,
            RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"))),
            DubDiskCache(tmp.newFolder("dub")),
            "ru",
            prefetchAheadMs = 999_999L,
        )
        val ducking = DubDuckingController(engine, duckedVolume = 0.2f, fadeMs = 0L)
        val audioPlayer = FakeAudioPlayer()
        val enhancer = PlaybackEnhancer(engine, dub = dub, ducking = ducking, audioPlayer = audioPlayer)

        enhancer.loadDubLines(listOf(DubLine(1, 0L, 2000L, "Привет", speaker = "A")))
        val state = enhancer.onTick(positionMs = 500L, isPlaying = true, nowMs = 0L)

        assertTrue(audioPlayer.isPlaying)
        assertEquals(1, audioPlayer.playedClip!!.line.index)
        assertEquals(0.2f, engine.lastVolume, 0.001f)
        assertEquals(1, state.dubProgress!!.synthesized)
    }

    @Test
    fun onTickDoesNotRestartSameClipOnConsecutiveTicks() = runTest {
        val engine = FakePlayerEngine()
        val provider = FakeTtsProvider()
        val dub = DubCoordinator(
            provider,
            RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"))),
            DubDiskCache(tmp.newFolder("dub2")),
            "ru",
            prefetchAheadMs = 999_999L,
        )
        val audioPlayer = FakeAudioPlayer()
        val enhancer = PlaybackEnhancer(engine, dub = dub, audioPlayer = audioPlayer)
        enhancer.loadDubLines(listOf(DubLine(1, 0L, 2000L, "Привет")))

        enhancer.onTick(500L, true, 0L)
        audioPlayer.stopCalls = 0 // reset after first natural stop() call inside updateDubPlayback, if any
        enhancer.onTick(1000L, true, 100L)

        assertEquals(0, audioPlayer.stopCalls)
    }

    @Test
    fun onTickStopsDubPlaybackWhenLeavingLineInterval() = runTest {
        val engine = FakePlayerEngine()
        val provider = FakeTtsProvider()
        val dub = DubCoordinator(
            provider,
            RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"))),
            DubDiskCache(tmp.newFolder("dub3")),
            "ru",
            prefetchAheadMs = 999_999L,
        )
        val ducking = DubDuckingController(engine, duckedVolume = 0.2f, fadeMs = 0L)
        val audioPlayer = FakeAudioPlayer()
        val enhancer = PlaybackEnhancer(engine, dub = dub, ducking = ducking, audioPlayer = audioPlayer)
        enhancer.loadDubLines(listOf(DubLine(1, 0L, 2000L, "Привет")))

        enhancer.onTick(500L, true, 0L)
        assertTrue(audioPlayer.isPlaying)

        enhancer.onTick(5000L, true, 100L)

        assertFalse(audioPlayer.isPlaying)
        assertEquals(1f, engine.lastVolume, 0.001f)
    }

    @Test
    fun dubLinesFromAsrConvertsCuesPreservingTimingAndText() = runTest {
        val engine = FakePlayerEngine()
        val asr = AsrCoordinator(
            FakeAsrEngine(),
            FakeAudioSource(),
            AsrDiskCache(tmp.newFolder("asr2")),
            windowMs = 30_000L,
            prefetchAheadMs = 5_000L,
        )
        val enhancer = PlaybackEnhancer(engine, asr = asr)
        enhancer.startEpisode("video-2", 30_000L, "ja")
        enhancer.onTick(0L, true, 0L)
        val doc = requireNotNull(asr.currentDocument())

        val lines = enhancer.dubLinesFromAsr(doc)

        assertEquals(doc.cues.size, lines.size)
        assertEquals(doc.cues.first().text, lines.first().text)
        assertEquals(doc.cues.first().startMs, lines.first().startMs)
    }

    @Test
    fun stopDubPlaybackForcesStopAndClearsCurrentClip() = runTest {
        val engine = FakePlayerEngine()
        val audioPlayer = FakeAudioPlayer()
        val enhancer = PlaybackEnhancer(engine, audioPlayer = audioPlayer)

        enhancer.stopDubPlayback()

        assertEquals(1, audioPlayer.stopCalls)
        assertNull(audioPlayer.playedClip)
    }
}
