package dev.anime.player.skip.net

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CachingAniListMalMapperTest {

    @Test
    fun resolvesMalIdFromResponse() = runTest {
        val mapper = CachingAniListMalMapper(
            post = { """{"data":{"Media":{"id":21519,"idMal":32281}}}""" },
        )
        assertEquals(32281, mapper.malIdFor(21519))
    }

    @Test
    fun cachesResultAndDoesNotRepeatRequest() = runTest {
        var calls = 0
        val mapper = CachingAniListMalMapper(
            post = { calls++; """{"data":{"Media":{"id":21519,"idMal":32281}}}""" },
        )
        mapper.malIdFor(21519)
        mapper.malIdFor(21519)
        mapper.malIdFor(21519)
        assertEquals(1, calls)
    }

    @Test
    fun returnsNullWhenMediaMissingOrRequestFails() = runTest {
        val missing = CachingAniListMalMapper(post = { """{"data":{"Media":null}}""" })
        assertNull(missing.malIdFor(999999))

        val failed = CachingAniListMalMapper(post = { null })
        assertNull(failed.malIdFor(1))
    }

    @Test
    fun cachesNullResultsToo() = runTest {
        var calls = 0
        val mapper = CachingAniListMalMapper(post = { calls++; null })
        mapper.malIdFor(1)
        mapper.malIdFor(1)
        assertEquals(1, calls)
    }
}
