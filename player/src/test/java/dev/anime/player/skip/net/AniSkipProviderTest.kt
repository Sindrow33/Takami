package dev.anime.player.skip.net

import dev.anime.player.skip.SkipType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniSkipProviderTest {

    private val foundJson = """
        {
          "found": true,
          "results": [
            {"interval": {"startTime": 12.5, "endTime": 102.0}, "skipType": "op"},
            {"interval": {"startTime": 1320.0, "endTime": 1400.5}, "skipType": "ed"},
            {"interval": {"startTime": 0.0, "endTime": 30.0}, "skipType": "recap"},
            {"interval": {"startTime": 5.0, "endTime": 5.0}, "skipType": "op"}
          ],
          "message": "ok",
          "statusCode": 200
        }
    """.trimIndent()

    private val notFoundJson = """{"found": false, "results": [], "statusCode": 404}"""

    @Test
    fun parsesFoundResultsAndDropsZeroLengthSegments() = runTest {
        var requestedUrl: String? = null
        val provider = AniSkipProvider(fetch = { url -> requestedUrl = url; foundJson })

        val segments = provider.segments(malId = 32281, episode = 1, durationMs = 1_440_000L)

        assertEquals(3, segments.size)
        assertEquals(SkipType.OP, segments[0].type)
        assertEquals(12_500L, segments[0].startMs)
        assertEquals(102_000L, segments[0].endMs)
        assertEquals(SkipType.RECAP, segments[1].type)
        assertEquals(SkipType.ED, segments[2].type)
        assertTrue(requestedUrl!!.contains("/32281/1"))
        assertTrue(requestedUrl!!.contains("episodeLength=1440.0"))
    }

    @Test
    fun returnsEmptyWhenNotFound() = runTest {
        val provider = AniSkipProvider(fetch = { notFoundJson })
        assertEquals(emptyList<Any>(), provider.segments(1, 1, 1_000_000L))
    }

    @Test
    fun returnsEmptyOnNullOrInvalidBody() = runTest {
        val providerNull = AniSkipProvider(fetch = { null })
        assertEquals(emptyList<Any>(), providerNull.segments(1, 1, 1_000_000L))

        val providerGarbage = AniSkipProvider(fetch = { "not json" })
        assertEquals(emptyList<Any>(), providerGarbage.segments(1, 1, 1_000_000L))
    }

    @Test
    fun returnsEmptyWhenMalIdOrEpisodeMissing() = runTest {
        val provider = AniSkipProvider(fetch = { foundJson })
        assertEquals(emptyList<Any>(), provider.segments(null, 1, 1_000_000L))
        assertEquals(emptyList<Any>(), provider.segments(1, 0, 1_000_000L))
    }
}
