package com.mangareader.translate.render

import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Vertical (top-to-bottom glyphs, right-to-left columns) text layout for
 * CJK vertical typesetting — the writing mode where §7 notes ML Kit's OCR
 * "tends to struggle" (which is why the Quality profile's VLM path exists
 * for this case), but rendering the *translated, horizontal* text is a
 * separate concern from OCR-ing the *original, vertical* text. If a
 * translated block is marked [com.mangareader.translate.api.TextBlock.vertical]
 * (i.e. the destination language itself should be typeset vertically —
 * relevant when translating between two CJK languages, or when the app
 * offers a "keep vertical" stylistic option), this class produces the
 * column layout. For the common case of translating INTO a
 * horizontally-read language (English, etc.), `:translate:render`'s
 * orchestration simply calls [PolygonTextLayout] instead — vertical
 * source text does not imply vertical destination text.
 */
object VerticalTextLayout {

    data class Column(val text: String, val originX: Float)

    data class FitResult(
        val fontSizePx: Float,
        val columns: List<Column>,
        val columnWidthPx: Float,
        val originY: Float,
    )

    fun fit(
        text: String,
        boxPx: RectF,
        basePaint: Paint,
        maxFontSizePx: Float = 48f,
        minFontSizePx: Float = 8f,
        marginFraction: Float = 0.08f,
    ): FitResult {
        val marginX = boxPx.width() * marginFraction
        val marginY = boxPx.height() * marginFraction
        val usableWidth = max(1f, boxPx.width() - 2 * marginX)
        val usableHeight = max(1f, boxPx.height() - 2 * marginY)

        var lo = minFontSizePx
        var hi = maxFontSizePx
        var bestSize = minFontSizePx
        var bestColumns: List<Column> = emptyList()
        var bestColumnWidth = minFontSizePx * 1.2f

        repeat(10) {
            val mid = (lo + hi) / 2f
            val columnWidth = mid * 1.2f
            val charsPerColumn = max(1, (usableHeight / mid).toInt())
            val columnCount = (text.length + charsPerColumn - 1) / charsPerColumn
            val totalWidth = columnCount * columnWidth
            if (totalWidth <= usableWidth) {
                bestSize = mid
                bestColumnWidth = columnWidth
                bestColumns = buildColumns(text, charsPerColumn, columnCount, boxPx.right - marginX, columnWidth)
                lo = mid
            } else {
                hi = mid
            }
        }

        return FitResult(bestSize, bestColumns, bestColumnWidth, boxPx.top + marginY)
    }

    private fun buildColumns(
        text: String,
        charsPerColumn: Int,
        columnCount: Int,
        rightEdgeX: Float,
        columnWidth: Float,
    ): List<Column> {
        // Columns advance right-to-left: the first column of a vertical
        // CJK block sits at the RIGHT edge, subsequent columns move left.
        val columns = ArrayList<Column>(columnCount)
        for (col in 0 until columnCount) {
            val start = col * charsPerColumn
            val end = min(text.length, start + charsPerColumn)
            if (start >= end) break
            val columnText = text.substring(start, end)
            val originX = rightEdgeX - (col + 1) * columnWidth
            columns.add(Column(columnText, originX))
        }
        return columns
    }
}
