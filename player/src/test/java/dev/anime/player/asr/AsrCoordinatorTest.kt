package dev.anime.player.asr

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private class FakeAudioSource : AudioSource {
    var callCount = 0
    override suspend fun pcm16(startMs: Long, endMs: Long, sampleRateHz: Int): ShortArray {
        callCount++
        return ShortArray(1)
    }
}

/** Возвращает один сегмент за окно, с текстом = номер вызова, времена относительны окну. */
private class FakeAsrEngine(
    override val fingerprint: String = "fake-1.0",
    override val sampleRateHz: Int = 16000,
    override val backend: AsrBackend = AsrBackend.WhisperCpp,
) : AsrEngine {
    var calls = 0
    override suspend fun transcribe(
        pcm16: ShortArray,
        sampleRateHz: Int,
        offsetMs: Long,
        language: String?,
    ): List<AsrSegment> {
        calls++
        return listOf(AsrSegment(0L, 1000L, "segment-$calls"))
    }
}

class AsrCoordinatorTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    private fun newCoordinator(
        engine: FakeAsrEngine = FakeAsrEngine(),
        audio: FakeAudioSource = FakeAudioSource(),
        windowMs: Long = 20_000L,
        prefetchAheadMs: Long = 45_000L,
    ) = Triple(
        AsrCoordinator(
            engine = engine,
            audioSource = audio,
            cache = AsrDiskCache(tmp.newFolder("asr-cache")),
            windowMs = windowMs,
            prefetchAheadMs = prefetchAheadMs,
        ),
        engine,
        audio,
    )

    @Test
    fun advanceToGeneratesOnlyWindowsWithinPrefetchWindow() = runTest {
        val (coordinator, engine, audio) = newCoordinator(windowMs = 20_000L, prefetchAheadMs = 10_000L)
        coordinator.start("video-1", durationMs = 120_000L, language = "ja")

        // playback at 0 + prefetch 10s -> target 10s -> only first 20s window needed.
        val progress = coordinator.advanceTo(0L)

        assertEquals(1, engine.calls)
        assertEquals(1, audio.callCount)
        assertEquals(20_000L, progress.generatedUpToMs)
    }

    @Test
    fun advanceToIsIdempotentWhenAlreadyCoveringTarget() = runTest {
        val (coordinator, engine, _) = newCoordinator(windowMs = 20_000L, prefetchAheadMs = 5_000L)
        coordinator.start("video-1", 60_000L, "ja")

        coordinator.advanceTo(0L)
        val callsAfterFirst = engine.calls
        coordinator.advanceTo(0L)

        assertEquals(callsAfterFirst, engine.calls)
    }

    @Test
    fun generateAllProducesFullDocumentAndPersistsToCache() = runTest {
        val (coordinator, _, _) = newCoordinator(windowMs = 30_000L, prefetchAheadMs = 0L)
        coordinator.start("video-2", 90_000L, "ja")

        val progress = coordinator.generateAll()

        assertEquals(AsrGenerationStage.Complete, progress.stage)
        val doc = coordinator.currentDocument()
        assertEquals(3, doc.cues.size)
        assertTrue(doc.cues.all { it.text.startsWith("segment-") })
    }

    @Test
    fun startReturnsCompleteImmediatelyWhenCacheHasFullResult() = runTest {
        val cacheDir = tmp.newFolder("asr-cache-2")
        val engine = FakeAsrEngine()
        val audio = FakeAudioSource()
        val cache = AsrDiskCache(cacheDir)
        val coordinator1 = AsrCoordinator(engine, audio, cache, windowMs = 30_000L, prefetchAheadMs = 0L)
        coordinator1.start("video-3", 30_000L, "ja")
        coordinator1.generateAll()
        val callsAfterFirstRun = engine.calls

        // New coordinator instance, same cache dir/key -> should hit cache, no new transcribe calls.
        val coordinator2 = AsrCoordinator(engine, audio, cache, windowMs = 30_000L, prefetchAheadMs = 0L)
        val startProgress = coordinator2.start("video-3", 30_000L, "ja")

        assertEquals(AsrGenerationStage.Complete, startProgress.stage)
        assertEquals(callsAfterFirstRun, engine.calls)
        assertEquals(1, coordinator2.currentDocument().cues.size)
    }

    @Test
    fun normalizeSegmentsAddsWindowOffsetOnlyWhenRelative() {
        val segs = listOf(AsrSegment(0L, 1000L, "a"))
        val abs = normalizeSegments(segs, windowOffsetMs = 5000L, timesAreRelative = false)
        val rel = normalizeSegments(segs, windowOffsetMs = 5000L, timesAreRelative = true)

        assertEquals(0L, abs[0].startMs)
        assertEquals(5000L, rel[0].startMs)
        assertEquals(6000L, rel[0].endMs)
    }
}
