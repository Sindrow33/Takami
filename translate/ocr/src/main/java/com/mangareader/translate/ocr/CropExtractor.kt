package com.mangareader.translate.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import com.mangareader.translate.api.RawBlock

/**
 * Cuts per-block crops out of the page's analysis bitmap (§6/§7), used as
 * input to both [MlKitOcrEngine] and (batched) VLM OCR+translate calls.
 *
 * Crops are axis-aligned rectangular expansions of each block's bbox with
 * a small margin (helps OCR context for glyphs right at the polygon edge)
 * — NOT masked to the polygon shape, since most OCR models expect a
 * rectangular input and perform worse on a masked/irregular one.
 */
object CropExtractor {

    private const val MARGIN_FRACTION = 0.08f

    fun extract(pageBitmap: Bitmap, blocks: List<RawBlock>): List<Bitmap> =
        blocks.map { block -> extractOne(pageBitmap, block.bboxPx) }

    private fun extractOne(pageBitmap: Bitmap, bboxPx: RectF): Bitmap {
        val marginX = bboxPx.width() * MARGIN_FRACTION
        val marginY = bboxPx.height() * MARGIN_FRACTION
        val left = (bboxPx.left - marginX).coerceIn(0f, pageBitmap.width.toFloat())
        val top = (bboxPx.top - marginY).coerceIn(0f, pageBitmap.height.toFloat())
        val right = (bboxPx.right + marginX).coerceIn(0f, pageBitmap.width.toFloat())
        val bottom = (bboxPx.bottom + marginY).coerceIn(0f, pageBitmap.height.toFloat())

        val w = (right - left).toInt().coerceAtLeast(1)
        val h = (bottom - top).toInt().coerceAtLeast(1)
        return Bitmap.createBitmap(pageBitmap, left.toInt(), top.toInt(), w, h)
    }

    /**
     * Rasterizes a block's normalized polygon into an 8-bit alpha mask at
     * [width]x[height] (analysis resolution), dilated by [dilatePx] to
     * fully cover anti-aliased glyph edges — the mask consumed by
     * [com.mangareader.translate.onnx.FastFillInpainter] /
     * [com.mangareader.translate.onnx.LamaInpainter].
     */
    fun rasterizeMask(polygonPx: FloatArray, width: Int, height: Int, dilatePx: Float = 3f): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val path = Path()
        for (i in polygonPx.indices step 2) {
            if (i == 0) path.moveTo(polygonPx[0], polygonPx[1]) else path.lineTo(polygonPx[i], polygonPx[i + 1])
        }
        path.close()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            maskFilter = android.graphics.BlurMaskFilter(dilatePx, android.graphics.BlurMaskFilter.Blur.SOLID)
        }
        canvas.drawPath(path, paint)
        return mask
    }
}
