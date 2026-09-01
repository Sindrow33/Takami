package dev.anime.player.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmTransformsTest {

    @Test
    fun averagesChannelsIntoMono() {
        val stereo = shortArrayOf(100, 200, -100, -200, 0, 0)
        assertArrayEquals(shortArrayOf(150, -150, 0), PcmTransforms.toMono(stereo, 2))
    }

    @Test
    fun monoInputIsReturnedUnchanged() {
        val mono = shortArrayOf(1, 2, 3)
        assertTrue(mono === PcmTransforms.toMono(mono, 1))
    }

    @Test
    fun keepsPanneddialogueThatOneChannelWouldLose() {
        // Речь только в правом канале: «взять левый» дало бы тишину.
        val stereo = shortArrayOf(0, 1000, 0, 1000)
        val mono = PcmTransforms.toMono(stereo, 2)
        assertEquals(500, mono[0].toInt())
        assertFalse(PcmTransforms.isSilence(mono))
    }

    @Test
    fun downsamplesToTheExpectedLength() {
        val input = ShortArray(48_000) { (it % 100).toShort() }
        val out = PcmTransforms.resample(input, 48_000, 16_000)
        assertEquals(16_000, out.size)
    }

    @Test
    fun upsamplesToTheExpectedLength() {
        val input = ShortArray(8_000) { 1 }
        assertEquals(16_000, PcmTransforms.resample(input, 8_000, 16_000).size)
    }

    @Test
    fun sameRateIsANoOp() {
        val input = shortArrayOf(5, 6, 7)
        assertTrue(input === PcmTransforms.resample(input, 16_000, 16_000))
    }

    @Test
    fun interpolatesBetweenNeighbours() {
        // 0, 100 при удвоении частоты -> 0, 50, 100, 100
        val out = PcmTransforms.resample(shortArrayOf(0, 100), 8_000, 16_000)
        assertEquals(4, out.size)
        assertEquals(0, out[0].toInt())
        assertEquals(50, out[1].toInt())
    }

    @Test
    fun invalidRatesReturnEmptyInsteadOfCrashing() {
        assertEquals(0, PcmTransforms.resample(shortArrayOf(1, 2), 0, 16_000).size)
        assertEquals(0, PcmTransforms.resample(shortArrayOf(1, 2), 16_000, 0).size)
    }

    @Test
    fun samplesForWindowMatchesRate() {
        assertEquals(16_000, PcmTransforms.samplesFor(1_000L, 16_000))
        assertEquals(320_000, PcmTransforms.samplesFor(20_000L, 16_000))
        assertEquals(0, PcmTransforms.samplesFor(-5L, 16_000))
    }

    @Test
    fun padsShortWindowSoTimecodesDoNotDrift() {
        val out = PcmTransforms.fitExact(shortArrayOf(1, 2, 3), 5)
        assertArrayEquals(shortArrayOf(1, 2, 3, 0, 0), out)
    }

    @Test
    fun trimsLongWindowToTheExactSize() {
        val out = PcmTransforms.fitExact(shortArrayOf(1, 2, 3, 4, 5), 3)
        assertArrayEquals(shortArrayOf(1, 2, 3), out)
    }

    @Test
    fun detectsSilenceAndSpeech() {
        assertTrue(PcmTransforms.isSilence(ShortArray(100)))
        assertTrue(PcmTransforms.isSilence(ShortArray(0)))
        assertFalse(PcmTransforms.isSilence(ShortArray(100) { 8000 }))
    }

    private fun assertArrayEquals(expected: ShortArray, actual: ShortArray) {
        assertEquals(expected.toList(), actual.toList())
    }
}
