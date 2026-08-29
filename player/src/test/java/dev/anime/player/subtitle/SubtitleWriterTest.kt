package dev.anime.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleWriterTest {

    private fun doc() = SubtitleDocument(
        listOf(
            SubtitleCue(1, 0L, 1500L, "Привет"),
            SubtitleCue(2, 3_661_200L, 3_662_500L, "Через час"),
        ),
    )

    @Test
    fun writesVttWithHeaderAndDotMillisSeparator() {
        val out = SubtitleWriter.write(doc().copy(format = SubtitleFormat.Vtt))

        assertTrue(out.startsWith("WEBVTT\n\n"))
        assertTrue(out.contains("00:00:00.000 --> 00:00:01.500"))
        assertTrue(out.contains("01:01:01.200 --> 01:01:02.500"))
        assertTrue(out.contains("Привет"))
    }

    @Test
    fun writesSrtWithCommaMillisSeparatorAndNoHeader() {
        val out = SubtitleWriter.write(doc().copy(format = SubtitleFormat.Srt))

        assertTrue(!out.startsWith("WEBVTT"))
        assertTrue(out.contains("00:00:00,000 --> 00:00:01,500"))
        assertTrue(out.contains("01:01:01,200 --> 01:01:02,500"))
    }

    @Test
    fun cueAtFindsCueContainingTimestampInclusiveStartExclusiveEnd() {
        val document = doc()
        assertEquals("Привет", document.cueAt(0L)?.text)
        assertEquals("Привет", document.cueAt(1499L)?.text)
        assertEquals(null, document.cueAt(1500L))
    }

    @Test
    fun negativeTimestampsAreClampedToZero() {
        val out = SubtitleWriter.write(SubtitleDocument(listOf(SubtitleCue(1, -500L, 1000L, "x"))))
        assertTrue(out.contains("00:00:00.000 --> 00:00:01.000"))
    }
}
