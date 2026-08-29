package dev.anime.player.dub

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private class FakeTtsProvider(override val id: String = "fake", override val fingerprint: String = "fake-1.0") :
    TtsProvider {
    var calls = 0
    override suspend fun synthesize(text: String, voice: TtsVoice): TtsProvider.SynthesisOutput {
        calls++
        return TtsProvider.SynthesisOutput(
            audio = text.toByteArray(Charsets.UTF_8),
            mimeType = "audio/wav",
            durationMs = (text.length * 60L).coerceAtLeast(500L),
        )
    }
}

class DubCoordinatorTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    private fun lines() = listOf(
        DubLine(1, 0L, 2000L, "Привет", speaker = "A"),
        DubLine(2, 30_000L, 33_000L, "Как дела", speaker = "B"),
        DubLine(3, 120_000L, 124_000L, "Пока", speaker = "A"),
    )

    @Test
    fun advanceToOnlySynthesizesLinesWithinPrefetchWindow() = runTest {
        val provider = FakeTtsProvider()
        val coordinator = DubCoordinator(
            provider = provider,
            voiceMapper = RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"), TtsVoice("v2", "ru"))),
            cache = DubDiskCache(tmp.newFolder("dub-cache")),
            languageCode = "ru",
            prefetchAheadMs = 10_000L,
        )
        coordinator.load(lines())

        val progress = coordinator.advanceTo(0L)

        assertEquals(1, progress.synthesized)
        assertEquals(3, progress.total)
        assertEquals(1, provider.calls)
        assertNotNull(coordinator.clipFor(1))
        assertNull(coordinator.clipFor(2))
    }

    @Test
    fun advanceToDoesNotResynthesizeAlreadySynthesizedLines() = runTest {
        val provider = FakeTtsProvider()
        val coordinator = DubCoordinator(
            provider = provider,
            voiceMapper = RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"))),
            cache = DubDiskCache(tmp.newFolder("dub-cache2")),
            languageCode = "ru",
            prefetchAheadMs = 10_000L,
        )
        coordinator.load(lines())
        coordinator.advanceTo(0L)
        val callsAfterFirst = provider.calls
        coordinator.advanceTo(0L)

        assertEquals(callsAfterFirst, provider.calls)
    }

    @Test
    fun cacheHitAvoidsCallingProviderAgainAcrossCoordinatorInstances() = runTest {
        val provider = FakeTtsProvider()
        val cache = DubDiskCache(tmp.newFolder("dub-cache3"))
        val mapper = RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru")))

        val coordinator1 = DubCoordinator(provider, mapper, cache, "ru", prefetchAheadMs = 999_999L)
        coordinator1.load(lines())
        coordinator1.generateAll()
        val callsAfterFirstRun = provider.calls
        assertEquals(3, callsAfterFirstRun)

        val coordinator2 = DubCoordinator(provider, RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"))), cache, "ru", prefetchAheadMs = 999_999L)
        coordinator2.load(lines())
        coordinator2.generateAll()

        assertEquals(callsAfterFirstRun, provider.calls)
        assertNotNull(coordinator2.clipFor(1))
    }

    @Test
    fun clipAtReturnsClipCoveringGivenPosition() = runTest {
        val provider = FakeTtsProvider()
        val coordinator = DubCoordinator(
            provider,
            RoundRobinVoiceMapper(listOf(TtsVoice("v1", "ru"))),
            DubDiskCache(tmp.newFolder("dub-cache4")),
            "ru",
            prefetchAheadMs = 999_999L,
        )
        coordinator.load(lines())
        coordinator.generateAll()

        assertNotNull(coordinator.clipAt(1000L))
        assertNull(coordinator.clipAt(10_000L))
        assertNotNull(coordinator.clipAt(31_000L))
    }
}
