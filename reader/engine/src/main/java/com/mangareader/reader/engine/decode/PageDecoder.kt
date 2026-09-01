package com.mangareader.reader.engine.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.os.Build
import com.mangareader.core.model.EdgeColors
import com.mangareader.core.model.PageKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The dual-decode strategy required by §6:
 *
 *  - [decodeForDisplay] returns a HARDWARE-config bitmap when possible.
 *    HARDWARE bitmaps are GPU-resident and cheap to draw/scale, but their
 *    pixels cannot be read back on the CPU (needed for dHash, edge colors,
 *    OCR). This is the bitmap the [reader.ui] View actually shows.
 *
 *  - [decodeForAnalysis] performs a SEPARATE, software (ARGB_8888) decode
 *    downsampled so the longer side is ~[analysisMaxDimension] px, used for
 *    dHash + edge-color sampling + (later) OCR/detection input. This
 *    bitmap is released immediately after use — it is never held onto or
 *    reused as "the display bitmap made readable", per §6's explicit
 *    instruction not to try to repurpose the screen bitmap.
 *
 * Bounds-only decoding ([readBounds]) is used first so callers can make
 * sizing/inSampleSize decisions without paying for a full decode.
 */
object PageDecoder {

    const val DEFAULT_ANALYSIS_MAX_DIMENSION = 1280

    data class Bounds(val width: Int, val height: Int)

    data class AnalysisResult(
        val pageKey: PageKey,
        val edgeColors: EdgeColors,
        /** The software bitmap used to compute the above; caller must recycle. */
        val bitmap: Bitmap,
    )

    suspend fun readBounds(file: File): Bounds = withContext(Dispatchers.IO) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        Bounds(opts.outWidth, opts.outHeight)
    }

    /**
     * Decodes the full page for on-screen display. Uses HARDWARE config on
     * API 26+ where the destination is guaranteed to be drawn via a
     * Canvas/RenderNode path that supports it (the reader's custom View
     * draws through a regular hardware-accelerated Canvas — see
     * `:reader:ui`). Falls back to ARGB_8888 below API 26 or when hardware
     * bitmaps are explicitly disabled (e.g. for the region-decoder tiling
     * path, which needs mutable/software tiles it can composite text onto).
     */
    suspend fun decodeForDisplay(file: File, allowHardware: Boolean = true): Bitmap =
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply {
                if (allowHardware && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    inPreferredConfig = Bitmap.Config.HARDWARE
                } else {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
                ?: error("Failed to decode page: ${file.absolutePath}")
        }

    /**
     * Software decode downsampled for analysis (dHash, edge colors, OCR
     * input). Always ARGB_8888, always CPU-readable, always independent of
     * whatever bitmap [decodeForDisplay] produced.
     */
    suspend fun decodeForAnalysis(
        file: File,
        maxDimension: Int = DEFAULT_ANALYSIS_MAX_DIMENSION,
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val bounds = readBounds(file)
        val longSide = maxOf(bounds.width, bounds.height)
        var sampleSize = 1
        while (longSide / (sampleSize * 2) >= maxDimension) sampleSize *= 2

        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize
        }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: error("Failed to decode page for analysis: ${file.absolutePath}")

        val pageKey = DHash.pageKeyOf(bmp, bounds.width, bounds.height)
        val edgeColors = EdgeColorSampler.sample(bmp)
        AnalysisResult(pageKey = pageKey, edgeColors = edgeColors, bitmap = bmp)
    }

    /**
     * Opens a [BitmapRegionDecoder] for tiled decoding of very tall webtoon
     * strips (§6: "for webtoon don't load the whole strip, cut it into
     * ~2000px tiles via BitmapRegionDecoder"). Caller is responsible for
     * closing/recycling via [BitmapRegionDecoder.recycle].
     */
    @Suppress("DEPRECATION")
    fun openRegionDecoder(file: File): BitmapRegionDecoder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(file.absolutePath)
        } else {
            BitmapRegionDecoder.newInstance(file.absolutePath, false)
        } ?: error("Failed to open region decoder: ${file.absolutePath}")
}
