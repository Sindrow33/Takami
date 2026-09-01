package com.mangareader.translate.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import com.mangareader.translate.api.BlockKind
import com.mangareader.translate.api.SfxPolicy
import com.mangareader.translate.api.TextBlock
import com.mangareader.translate.api.TranslationMode

/**
 * Draws a single [TextBlock] onto the text layer's Canvas, in PAGE-LOCAL
 * pixel coordinates (the caller — `:reader:ui`'s overlay View — has
 * already applied the shared image/text transformation matrix per §6, so
 * this class only ever draws in a page's own coordinate space and never
 * touches Matrix/Canvas.save() bookkeeping beyond a per-block clip).
 *
 * Mode behaviour (§5.2):
 *  - OVERLAY: draw a semi-opaque backing shaped to the block's polygon,
 *    then the translated text on top. Art is never modified.
 *  - REPLACE: assume the caller has already composited the inpainted
 *    patch into the image layer; this class only draws crisp text with a
 *    thin outline/background chosen from [TextBlock.fgColor]/[TextBlock.bgColor]
 *    for legibility, no big translucent backing needed since the
 *    original art is gone underneath.
 *  - ORIGINAL: this class is not invoked at all — the text layer is
 *    fully hidden (§5.2 "art as is").
 */
object BlockPainter {

    private const val OVERLAY_BACKING_ALPHA = 190 // ~75% opaque backing, per "полупрозрачная подложка"

    fun draw(
        canvas: Canvas,
        block: TextBlock,
        pagePolygonPx: FloatArray,
        pageBboxPx: RectF,
        mode: TranslationMode,
        sfxPolicy: SfxPolicy,
        basePaint: TextPaint,
    ) {
        if (mode == TranslationMode.ORIGINAL) return
        if (block.kind == BlockKind.SFX && sfxPolicy == SfxPolicy.IGNORE) return

        canvas.save()
        try {
            // Clip to THIS page's bbox is enforced one level up by the
            // caller (the overlay must clip to page bounds per §6's
            // explicit "otherwise a bubble at a page's bottom edge would
            // spill onto the neighbour in the seamless feed" warning) —
            // here we additionally clip to the block's own polygon so
            // overlapping/adjacent blocks on the SAME page never bleed
            // into each other either.
            val clipPath = polygonPath(pagePolygonPx)
            canvas.clipPath(clipPath)

            when {
                block.kind == BlockKind.SFX && sfxPolicy == SfxPolicy.CAPTION -> drawSfxCaption(canvas, block, pageBboxPx, basePaint)
                mode == TranslationMode.OVERLAY -> drawOverlay(canvas, block, pageBboxPx, basePaint, clipPath)
                mode == TranslationMode.REPLACE -> drawReplace(canvas, block, pageBboxPx, basePaint)
            }
        } finally {
            canvas.restore()
        }
    }

    private fun drawOverlay(canvas: Canvas, block: TextBlock, bboxPx: RectF, basePaint: TextPaint, clipPath: Path) {
        val backingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = block.bgColor
            alpha = OVERLAY_BACKING_ALPHA
            style = Paint.Style.FILL
        }
        canvas.drawPath(clipPath, backingPaint)
        drawText(canvas, block, bboxPx, basePaint)
    }

    private fun drawReplace(canvas: Canvas, block: TextBlock, bboxPx: RectF, basePaint: TextPaint) {
        // Original erased already; just draw legible text, no extra backing.
        drawText(canvas, block, bboxPx, basePaint)
    }

    private fun drawText(canvas: Canvas, block: TextBlock, bboxPx: RectF, basePaint: TextPaint) {
        val paint = TextPaint(basePaint).apply { color = block.fgColor }
        if (block.vertical) {
            val fit = VerticalTextLayout.fit(block.dst, bboxPx, paint)
            for (column in fit.columns) {
                canvas.save()
                canvas.translate(column.originX, fit.originY)
                var y = 0f
                for (ch in column.text) {
                    canvas.drawText(ch.toString(), 0f, y, paint.apply { textSize = fit.fontSizePx })
                    y += fit.fontSizePx * 1.15f
                }
                canvas.restore()
            }
        } else {
            val fit = PolygonTextLayout.fitHorizontal(block.dst, bboxPx, paint)
            canvas.save()
            canvas.translate(fit.originX, fit.originY)
            fit.staticLayout.draw(canvas)
            canvas.restore()
        }
    }

    /** SFX-as-caption: small label placed just outside the effect, not redrawn in place (§7 setting). */
    private fun drawSfxCaption(canvas: Canvas, block: TextBlock, bboxPx: RectF, basePaint: TextPaint) {
        val paint = TextPaint(basePaint).apply {
            color = block.fgColor
            textSize = bboxPx.height().coerceAtMost(28f).coerceAtLeast(10f)
        }
        val captionY = bboxPx.bottom + paint.textSize
        canvas.drawText(block.dst, bboxPx.left, captionY, paint)
    }

    private fun polygonPath(polygonPx: FloatArray): Path {
        val path = Path()
        for (i in polygonPx.indices step 2) {
            if (i == 0) path.moveTo(polygonPx[0], polygonPx[1]) else path.lineTo(polygonPx[i], polygonPx[i + 1])
        }
        path.close()
        return path
    }
}
