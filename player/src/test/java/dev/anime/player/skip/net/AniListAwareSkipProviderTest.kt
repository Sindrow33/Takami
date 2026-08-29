package dev.anime.player.skip.net

import dev.anime.player.skip.SkipProvider
import dev.anime.player.skip.SkipSegment
import dev.anime.player.skip.SkipType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniListAwareSkipProviderTest {

    @Test
    fun mapsAniListIdBeforeDelegating() = runTest {
        var receivedMalId: Int? = null
        val fakeDelegate = object : SkipProvider {
            override val name = "fake"
            override suspend fun segments(malId: Int?, episode: Int, durationMs: Long): List<SkipSegment> {
                receivedMalId = malId
                return listOf(SkipSegment(SkipType.OP, 0L, 100L))
            }
        }
        val mapper = CachingAniListMalMapper(post = { """{"data":{"Media":{"id":21519,"idMal":32281}}}""" })
        val provider = AniListAwareSkipProvider(mapper = mapper, delegate = fakeDelegate)

        val result = provider.segments(21519, 1, 1_000_000L)

        assertEquals(32281, receivedMalId)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun returnsEmptyWhenMappingFails() = runTest {
        val mapper = CachingAniListMalMapper(post = { null })
        val delegate = object : SkipProvider {
            override val name = "fake"
            override suspend fun segments(malId: Int?, episode: Int, durationMs: Long) =
                error("должен быть недостижим без malId")
        }
        val provider = AniListAwareSkipProvider(mapper = mapper, delegate = delegate)
        assertEquals(emptyList<Any>(), provider.segments(999999, 1, 1_000_000L))
    }

    @Test
    fun returnsEmptyWhenAniListIdIsNull() = runTest {
        val provider = AniListAwareSkipProvider(
            mapper = CachingAniListMalMapper(post = { null }),
            delegate = object : SkipProvider {
                override val name = "fake"
                override suspend fun segments(malId: Int?, episode: Int, durationMs: Long) =
                    error("не должен вызываться")
            },
        )
        assertEquals(emptyList<Any>(), provider.segments(null, 1, 1000L))
    }
}
