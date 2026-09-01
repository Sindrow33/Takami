package com.mangareader.translate.onnx

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.mangareader.translate.api.Inpainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The cheap 90%-case erase path (§7): fill the block's polygon with the
 * bubble's own median background color, feathered at the mask edge so the
 * fill blends rather than leaving a hard-edged patch. This is a
 * millisecond, allocation-light operation — the intended default for
 * REPLACE mode on ordinary rectangular/oval speech bubbles.
 *
 * [LamaInpainter] is reserved for blocks this heuristic will visibly fail
 * on (busy backgrounds behind free-floating text, SFX drawn directly on
 * art) — `:translate:core`'s orchestrator decides which to call per
 * block, this class does not self-select.
 */
class FastFillInpainter(
    private val featherRadiusPx: Float = 6f,
) : Inpainter {

    /**
     * @param bmp the source crop containing the region to erase.
     * @param mask an 8-bit alpha mask, same size as [bmp]: 255 = erase this
     * pixel (belongs to text/background-fill area), 0 = keep original.
     * Typically the block's polygon rasterized with a couple of pixels of
     * dilation to fully cover anti-aliased glyph edges.
     */
    override suspend fun erase(bmp: Bitmap, mask: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        require(mask.width == bmp.width && mask.height == bmp.height) {
            "mask must match bitmap dimensions"
        }
        val fillColor = estimateBackgroundColor(bmp, mask)
        val result = bmp.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val featheredMask = feather(mask, featherRadiusPx)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }

        // Draw solid fill, then use the feathered mask's alpha as a
        // soft-edged erase-and-replace via SRC_IN compositing onto a
        // dedicated layer, then blend that layer over the original.
        val fillLayer = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val fillCanvas = Canvas(fillLayer)
        fillCanvas.drawColor(fillColor)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        fillCanvas.drawBitmap(featheredMask, 0f, 0f, maskPaint)

        canvas.drawBitmap(fillLayer, 0f, 0f, null)
        fillLayer.recycle()
        featheredMask.recycle()
        result
    }

    /** Samples background color from pixels just OUTSIDE the mask (the bubble fill, not the text). */
    private fun estimateBackgroundColor(bmp: Bitmap, mask: Bitmap): Int {
        val w = bmp.width
        val h = bmp.height
        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0L
        val stride = maxOf(1, (w * h) / 2000) // bounded sampling cost
        var i = 0
        while (i < w * h) {
            val x = i % w
            val y = i / w
            val maskAlpha = Color.alpha(mask.getPixel(x, y))
            if (maskAlpha < 40) { // clearly outside the erase region: sample it
                val p = bmp.getPixel(x, y)
                rSum += Color.red(p); gSum += Color.green(p); bSum += Color.blue(p)
                count++
            }
            i += stride
        }
        if (count == 0L) return Color.WHITE
        return Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
    }

    private fun feather(mask: Bitmap, radiusPx: Float): Bitmap {
        val result = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(mask, 0f, 0f, paint)
        return result
    }
}
