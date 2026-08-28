package moe.scenesearch.core.hash

/** Полутоновый кадр: pixels[y * width + x] в диапазоне 0..255. */
class GrayFrame(val width: Int, val height: Int, val pixels: IntArray) {
    init {
        require(width > 0 && height > 0) { "empty frame" }
        require(pixels.size >= width * height) { "pixels array too small" }
    }

    fun at(x: Int, y: Int): Int = pixels[y * width + x]
}

/**
 * Декод картинки в кадр. На Android реализуется через Bitmap,
 * на обычной JVM через ImageIO. Ядру платформа не важна.
 */
fun interface FrameDecoder {
    fun decode(bytes: ByteArray): GrayFrame?

    companion object {
        val None: FrameDecoder = FrameDecoder { null }
    }
}

object PerceptualHash {
    private const val W = 9
    private const val H = 8

    /** dHash: сравнение соседних пикселей по горизонтали, устойчив к яркости и сжатию. */
    fun dHash(frame: GrayFrame): Long {
        val small = resizeNearest(frame, W, H)
        var hash = 0L
        var bit = 0
        for (y in 0 until H) {
            for (x in 0 until W - 1) {
                if (small.at(x, y) > small.at(x + 1, y)) hash = hash or (1L shl bit)
                bit++
            }
        }
        return hash
    }

    fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun resizeNearest(src: GrayFrame, w: Int, h: Int): GrayFrame {
        val out = IntArray(w * h)
        for (y in 0 until h) {
            val sy = (y.toLong() * src.height / h).toInt().coerceIn(0, src.height - 1)
            for (x in 0 until w) {
                val sx = (x.toLong() * src.width / w).toInt().coerceIn(0, src.width - 1)
                out[y * w + x] = src.at(sx, sy)
            }
        }
        return GrayFrame(w, h, out)
    }
}

object Digest {
    fun sha256(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xFF
            sb.append("0123456789abcdef"[v shr 4])
            sb.append("0123456789abcdef"[v and 0x0F])
        }
        return sb.toString()
    }
}
