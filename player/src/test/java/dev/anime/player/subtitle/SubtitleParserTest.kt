package dev.anime.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleParserTest {

    @Test
    fun parsesSrt() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,500
            First line

            2
            00:00:04,000 --> 00:00:06,000
            Second line
            continued
        """.trimIndent()

        val doc = SubtitleParser.parse(srt)

        assertEquals(2, doc.cues.size)
        assertEquals(1_000L, doc.cues[0].startMs)
        assertEquals(3_500L, doc.cues[0].endMs)
        assertEquals("First line", doc.cues[0].text)
        assertEquals("Second line continued", doc.cues[1].text)
    }

    @Test
    fun parsesVttWithCueSettings() {
        val vtt = """
            WEBVTT

            00:00:02.000 --> 00:00:04.000 align:start line:90%
            Hello
        """.trimIndent()

        val doc = SubtitleParser.parse(vtt)
        assertEquals(1, doc.cues.size)
        assertEquals(2_000L, doc.cues[0].startMs)
        assertEquals(4_000L, doc.cues[0].endMs)
    }

    @Test
    fun acceptsTimesWithoutHours() {
        assertEquals(62_500L, SubtitleParser.parseTime("01:02.500"))
        assertEquals(3_600_000L, SubtitleParser.parseTime("01:00:00.000"))
        assertEquals(1_500L, SubtitleParser.parseTime("00:00:01,500"))
    }

    @Test
    fun rejectsGarbageTimes() {
        assertNull(SubtitleParser.parseTime(""))
        assertNull(SubtitleParser.parseTime("abc"))
        assertNull(SubtitleParser.parseTime("12"))
    }

    @Test
    fun stripsMarkupSoTtsDoesNotReadItAloud() {
        assertEquals("italic text", SubtitleParser.stripTags("<i>italic text</i>"))
        assertEquals("positioned", SubtitleParser.stripTags("{\\pos(100,200)}positioned"))
        assertEquals("plain", SubtitleParser.stripTags("plain"))
    }

    @Test
    fun skipsEmptyAndInvertedCues() {
        val srt = """
            1
            00:00:05,000 --> 00:00:01,000
            inverted

            2
            00:00:06,000 --> 00:00:07,000

            3
            00:00:08,000 --> 00:00:09,000
            good
        """.trimIndent()

        val doc = SubtitleParser.parse(srt)
        assertEquals(1, doc.cues.size)
        assertEquals("good", doc.cues[0].text)
    }

    @Test
    fun handlesWindowsLineEndings() {
        val srt = "1\r\n00:00:01,000 --> 00:00:02,000\r\nline\r\n"
        assertEquals(1, SubtitleParser.parse(srt).cues.size)
    }

    @Test
    fun emptyInputGivesEmptyDocument() {
        assertTrue(SubtitleParser.parse("").cues.isEmpty())
        assertTrue(SubtitleParser.parse("WEBVTT\n\n").cues.isEmpty())
    }

    @Test
    fun recognisesSupportedExtensions() {
        assertTrue(SubtitleParser.isSupported("a.srt"))
        assertTrue(SubtitleParser.isSupported("a.VTT"))
        assertFalse(SubtitleParser.isSupported("a.ass"))
        assertFalse(SubtitleParser.isSupported("a"))
    }

    @Test
    fun sidecarKeepsTheVideoName() {
        val names = SubtitleParser.sidecarNames("01 - start.mkv")
        assertTrue(names.contains("01 - start.srt"))
        assertTrue(names.contains("01 - start.vtt"))
    }

    @Test
    fun cueLookupFindsTheActiveLine() {
        val doc = SubtitleParser.parse(
            "1\n00:00:01,000 --> 00:00:03,000\nline\n"
        )
        assertEquals("line", doc.cueAt(2_000L)?.text)
        assertNull(doc.cueAt(5_000L))
    }
}
