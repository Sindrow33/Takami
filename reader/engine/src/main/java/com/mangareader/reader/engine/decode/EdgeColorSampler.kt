package com.mangareader.reader.engine.decode

import android.graphics.Bitmap
import android.graphics.Color
import com.mangareader.core.model.EdgeColors
import kotlin.math.max
import kotlin.math.min

/**
 * Samples the four edge colors of a page for the seam-hiding background
 * (§5.6b). Each edge is sampled over a small band of rows/columns (not a
 * single pixel line) and reduced by per-channel median, so a stray
 * speech-bubble border or a single bright pixel near the border cannot
 * skew the result — exactly the "not by one pixel" requirement in the
 * spec.
 */
object EdgeColorSampler {

    /** Fraction of the shorter dimension used as the sampling band thickness. */
    private const val BAND_FRACTION = 0.015f
    private const val MIN_BAND_PX = 3
    private const val MAX_BAND_PX = 24
    /** Stride to keep sampling cost bounded on very wide/tall pages. */
    private const val MAX_SAMPLES_PER_EDGE = 64

    fun sample(bitmap: Bitmap): EdgeColors {
        require(bitmap.config != Bitmap.Config.HARDWARE) {
            "EdgeColorSampler.sample requires a software-readable bitmap"
        }
        val w = bitmap.width
        val h = bitmap.height
        val band = min(MAX_BAND_PX, max(MIN_BAND_PX, (min(w, h) * BAND_FRACTION).toInt()))

        val top = sampleHorizontalBand(bitmap, yStart = 0, yEnd = min(band, h))
        val bottom = sampleHorizontalBand(bitmap, yStart = max(0, h - band), yEnd = h)
        val left = sampleVerticalBand(bitmap, xStart = 0, xEnd = min(band, w))
        val right = sampleVerticalBand(bitmap, xStart = max(0, w - band), xEnd = w)

        return EdgeColors(top = top, bottom = bottom, left = left, right = right)
    }

    private fun sampleHorizontalBand(bitmap: Bitmap, yStart: Int, yEnd: Int): Int {
        val w = bitmap.width
        val xStride = max(1, w / MAX_SAMPLES_PER_EDGE)
        val reds = ArrayList<Int>()
        val greens = ArrayList<Int>()
        val blues = ArrayList<Int>()
        var y = yStart
        while (y < yEnd) {
            var x = 0
            while (x < w) {
                val p = bitmap.getPixel(x, y)
                reds.add(Color.red(p)); greens.add(Color.green(p)); blues.add(Color.blue(p))
                x += xStride
            }
            y++
        }
        return medianColor(reds, greens, blues)
    }

    private fun sampleVerticalBand(bitmap: Bitmap, xStart: Int, xEnd: Int): Int {
        val h = bitmap.height
        val yStride = max(1, h / MAX_SAMPLES_PER_EDGE)
        val reds = ArrayList<Int>()
        val greens = ArrayList<Int>()
        val blues = ArrayList<Int>()
        var x = xStart
        while (x < xEnd) {
            var y = 0
            while (y < h) {
                val p = bitmap.getPixel(x, y)
                reds.add(Color.red(p)); greens.add(Color.green(p)); blues.add(Color.blue(p))
                y += yStride
            }
            x++
        }
        return medianColor(reds, greens, blues)
    }

    private fun medianColor(reds: List<Int>, greens: List<Int>, blues: List<Int>): Int {
        if (reds.isEmpty()) return Color.WHITE
        return Color.rgb(median(reds), median(greens), median(blues))
    }

    private fun median(values: List<Int>): Int {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
    }
}
