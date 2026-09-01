package com.mangareader.translate.render

import android.graphics.Paint
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.max
import kotlin.math.min

/**
 * Fits [TextBlock.dst] inside a block's polygon/bbox (§4/§6: "typography
 * inside the polygon"). Since Android has no built-in "shrink-to-fit
 * inside an arbitrary polygon" text layout, we approximate the usable
 * area as the polygon's axis-aligned bbox inset by a small margin — good
 * enough for the overwhelming majority of speech-bubble shapes (oval,
 * rounded-rect, roughly-convex) — and binary-search the largest font size
 * that lets [StaticLayout] wrap the text within that box without
 * overflowing vertically.
 *
 * Vertical text (§4 `TextBlock.vertical`) uses a distinct layout pass:
 * columns advance right-to-left, glyphs top-to-bottom — implemented in
 * [VerticalTextLayout] since [StaticLayout] has no vertical-writing mode.
 */
object PolygonTextLayout {

    data class FitResult(
        val fontSizePx: Float,
        val staticLayout: StaticLayout,
        val originX: Float,
        val originY: Float,
    )

    fun fitHorizontal(
        text: String,
        boxPx: RectF,
        basePaint: TextPaint,
        maxFontSizePx: Float = 64f,
        minFontSizePx: Float = 8f,
        marginFraction: Float = 0.08f,
    ): FitResult {
        val marginX = boxPx.width() * marginFraction
        val marginY = boxPx.height() * marginFraction
        val usableWidth = max(1f, boxPx.width() - 2 * marginX)
        val usableHeight = max(1f, boxPx.height() - 2 * marginY)

        var lo = minFontSizePx
        var hi = maxFontSizePx
        var best: StaticLayout? = null
        var bestSize = minFontSizePx

        // Binary search largest font size where wrapped text height <= usableHeight.
        repeat(12) {
            val mid = (lo + hi) / 2f
            val paint = TextPaint(basePaint).apply { textSize = mid }
            val layout = buildStaticLayout(text, paint, usableWidth.toInt())
            if (layout.height <= usableHeight) {
                best = layout
                bestSize = mid
                lo = mid
            } else {
                hi = mid
            }
        }

        val finalLayout = best ?: buildStaticLayout(
            text,
            TextPaint(basePaint).apply { textSize = minFontSizePx },
            usableWidth.toInt(),
        )

        val originX = boxPx.left + marginX + (usableWidth - finalLayout.width) / 2f
        val originY = boxPx.top + marginY + (usableHeight - finalLayout.height) / 2f

        return FitResult(bestSize, finalLayout, originX, originY)
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, max(1, width))
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.05f)
            .setIncludePad(false)
            .build()
}
