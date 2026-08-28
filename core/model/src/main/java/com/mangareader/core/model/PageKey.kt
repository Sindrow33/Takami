package com.mangareader.core.model

/**
 * Content-addressed identity of a manga page image.
 *
 * Format: `"d:<16 hex chars = 64-bit dHash>:<w>x<h>"`, e.g.
 * `"d:8f3ac10eec4a219b:1080x1536"`.
 *
 * Rationale (see project spec §3): everything related to translation is
 * cached by *pixel content*, not by URL. A perceptual difference-hash
 * (dHash, 64 bits, computed on a 9x8 grayscale thumbnail — see
 * `reader.engine.decode.DHash`) is stable across:
 *  - resizing (different CDN transforms of the same page),
 *  - re-encoding (JPEG vs WebP vs PNG of the same page),
 *  - mirror changes (same scan, different host).
 *
 * This buys three properties for free:
 *  1. A translation made while reading online is instantly valid after the
 *     chapter is downloaded for offline reading (same page, same hash).
 *  2. Switching mirrors does not invalidate the translation cache.
 *  3. The same chapter re-uploaded by a different source is translated once.
 *
 * [width] and [height] are the dimensions of the exact bitmap the hash was
 * computed from; they are carried alongside the hash string to let callers
 * do an aspect-ratio-gated Hamming-distance fallback match (see
 * [PageKey.matches]) without re-parsing image files.
 */
@JvmInline
value class PageKey(val v: String) {

    val hashHex: String get() = v.substringAfter("d:").substringBefore(":")

    val dimensions: Pair<Int, Int> get() {
        val dims = v.substringAfterLast(":")
        val w = dims.substringBefore("x").toIntOrNull() ?: 0
        val h = dims.substringAfter("x").toIntOrNull() ?: 0
        return w to h
    }

    val hashBits: Long get() = hashHex.toULong(16).toLong()

    override fun toString(): String = v

    companion object {
        private const val PREFIX = "d"

        fun of(dHash64: Long, width: Int, height: Int): PageKey =
            PageKey("$PREFIX:${dHash64.toULong().toString(16).padStart(16, '0')}:${width}x$height")

        fun parseOrNull(raw: String): PageKey? {
            val parts = raw.split(":")
            if (parts.size != 3 || parts[0] != PREFIX) return null
            if (parts[1].length != 16) return null
            val dims = parts[2].split("x")
            if (dims.size != 2 || dims[0].toIntOrNull() == null || dims[1].toIntOrNull() == null) return null
            return PageKey(raw)
        }

        /** Hamming distance between two 64-bit hashes, 0..64. */
        fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

        /**
         * Matching policy used by the translation cache (§3):
         * exact hash match, OR Hamming distance <= [maxDistance] gated by a
         * matching aspect ratio (within [aspectTolerance]) to avoid false
         * positives between unrelated pages that happen to hash close.
         */
        fun matches(
            a: PageKey,
            b: PageKey,
            maxDistance: Int = 4,
            aspectTolerance: Float = 0.03f,
        ): Boolean {
            if (a.hashHex == b.hashHex) return true
            val dist = hamming(a.hashBits, b.hashBits)
            if (dist > maxDistance) return false
            val (aw, ah) = a.dimensions
            val (bw, bh) = b.dimensions
            if (aw == 0 || ah == 0 || bw == 0 || bh == 0) return false
            val aspectA = aw.toFloat() / ah
            val aspectB = bw.toFloat() / bh
            return kotlin.math.abs(aspectA - aspectB) <= aspectTolerance
        }
    }
}
