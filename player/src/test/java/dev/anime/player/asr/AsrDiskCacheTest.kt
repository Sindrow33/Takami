package dev.anime.player.asr

import dev.anime.player.subtitle.SubtitleCue
import dev.anime.player.subtitle.SubtitleDocument
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AsrDiskCacheTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    @Test
    fun writeThenReadRoundTripsCuesAndTimings() = runTest {
        val cache = AsrDiskCache(tmp.newFolder("cache"))
        val key = AsrCacheKey("video-1", "whisper-1.0", "ja")
        val doc = SubtitleDocument(
            listOf(
                SubtitleCue(1, 0L, 1500L, "こんにちは"),
                SubtitleCue(2, 1500L, 3200L, "line two\nwrapped"),
            ),
        )

        cache.write(key, doc)
        val read = cache.read(key)

        requireNotNull(read)
        assertEquals(2, read.cues.size)
        assertEquals(0L, read.cues[0].startMs)
        assertEquals(1500L, read.cues[0].endMs)
        assertEquals("こんにちは", read.cues[0].text)
        assertEquals("line two\nwrapped", read.cues[1].text)
    }

    @Test
    fun readReturnsNullWhenMissing() = runTest {
        val cache = AsrDiskCache(tmp.newFolder("cache2"))
        assertNull(cache.read(AsrCacheKey("nope", "fp", "ja")))
    }

    @Test
    fun differentKeysDoNotCollide() = runTest {
        val cache = AsrDiskCache(tmp.newFolder("cache3"))
        val keyA = AsrCacheKey("video-a", "fp", "ja")
        val keyB = AsrCacheKey("video-b", "fp", "ja")
        cache.write(keyA, SubtitleDocument(listOf(SubtitleCue(1, 0L, 100L, "A"))))

        assertNull(cache.read(keyB))
        assertEquals("A", cache.read(keyA)?.cues?.first()?.text)
    }
}
