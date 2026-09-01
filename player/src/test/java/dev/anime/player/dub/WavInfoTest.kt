package dev.anime.player.dub

import org.junit.Assert.assertEquals
import org.junit.Test

class WavInfoTest {

    /** Канонический заголовок: [rate] Гц, [channels] каналов, 16 бит. */
    private fun header(rate: Int, channels: Int, bits: Int = 16): ByteArray {
        val h = ByteArray(WavInfo.HEADER_SIZE)
        writeShort(h, 22, channels)
        writeInt(h, 24, rate)
        writeShort(h, 34, bits)
        return h
    }

    @Test
    fun readsFormatFields() {
        val wav = header(22_050, 1) + ByteArray(100)
        assertEquals(22_050, WavInfo.sampleRate(wav))
        assertEquals(1, WavInfo.channels(wav))
        assertEquals(16, WavInfo.bitsPerSample(wav))
    }

    @Test
    fun computesDurationFromDataSize() {
        // 16 кГц, моно, 16 бит = 32 000 байт в секунду.
        val wav = header(16_000, 1) + ByteArray(32_000)
        assertEquals(1_000L, WavInfo.durationMs(wav))
    }

    @Test
    fun accountsForStereoAndBitDepth() {
        val wav = header(16_000, 2) + ByteArray(64_000)
        assertEquals(1_000L, WavInfo.durationMs(wav))
    }

    @Test
    fun headerOnlyMeansZeroDuration() {
        assertEquals(0L, WavInfo.durationMs(header(16_000, 1)))
        assertEquals(0L, WavInfo.durationMs(ByteArray(0)))
    }

    @Test
    fun brokenHeaderDoesNotCrash() {
        // Нулевая частота — не делим на ноль, а честно возвращаем 0.
        assertEquals(0L, WavInfo.durationMs(ByteArray(WavInfo.HEADER_SIZE + 100)))
        assertEquals(0, WavInfo.sampleRate(ByteArray(4)))
    }

    @Test
    fun zeroBitDepthFallsBackTo16() {
        val h = ByteArray(WavInfo.HEADER_SIZE)
        writeShort(h, 22, 1)
        writeInt(h, 24, 16_000)
        // bitsPerSample = 0 в заголовке
        assertEquals(1_000L, WavInfo.durationMs(h + ByteArray(32_000)))
    }

    private fun writeInt(b: ByteArray, offset: Int, value: Int) {
        b[offset] = (value and 0xFF).toByte()
        b[offset + 1] = ((value shr 8) and 0xFF).toByte()
        b[offset + 2] = ((value shr 16) and 0xFF).toByte()
        b[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShort(b: ByteArray, offset: Int, value: Int) {
        b[offset] = (value and 0xFF).toByte()
        b[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
}
