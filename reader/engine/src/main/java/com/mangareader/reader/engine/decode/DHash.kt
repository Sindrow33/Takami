package com.mangareader.reader.engine.decode

import android.graphics.Bitmap
import android.graphics.Color
import com.mangareader.core.model.PageKey

/**
 * Computes the 64-bit perceptual difference-hash (dHash) used to build a
 * [PageKey] (§3 of the spec).
 *
 * Algorithm: downscale to a 9x8 grayscale thumbnail, then for each row
 * compare each pixel to its right neighbour (9 columns → 8 comparisons per
 * row, 8 rows → 64 bits total). This is the classic dHash construction: it
 * is invariant to global brightness/contrast shifts and highly tolerant of
 * resizing and lossy re-encoding, which is exactly the robustness we need
 * across CDN transforms and mirrors.
 *
 * Cost: one small (9x8) software downscale — a few microseconds, safe to
 * run inline on every page decode as the spec requires.
 */
object DHash {

    private const val HASH_W = 9
    private const val HASH_H = 8

    /**
     * @param bitmap any decodable bitmap of the *original* page (before any
     * on-screen scaling matrix is applied). Must be readable pixel-by-pixel
     * — i.e. NOT a HARDWARE-config bitmap (see §6: hardware bitmaps cannot
     * be read back; always compute this from the software decode pass).
     */
    fun compute(bitmap: Bitmap): Long {
        require(bitmap.config != Bitmap.Config.HARDWARE) {
            "DHash.compute requires a software-readable bitmap; got HARDWARE config"
        }
        val thumb = Bitmap.createScaledBitmap(bitmap, HASH_W, HASH_H, true)
        var hash = 0L
        var bit = 63
        try {
            for (y in 0 until HASH_H) {
                var prevLuma = luma(thumb.getPixel(0, y))
                for (x in 1 until HASH_W) {
                    val luma = luma(thumb.getPixel(x, y))
                    if (prevLuma > luma) {
                        hash = hash or (1L shl bit)
                    }
                    prevLuma = luma
                    bit--
                }
            }
        } finally {
            if (thumb !== bitmap) thumb.recycle()
        }
        return hash
    }

    fun pageKeyOf(bitmap: Bitmap, originalWidth: Int, originalHeight: Int): PageKey =
        PageKey.of(compute(bitmap), originalWidth, originalHeight)

    private fun luma(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        // Standard luma coefficients (ITU-R BT.601); precision beyond this
        // is irrelevant for a difference hash.
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
