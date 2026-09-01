package com.mangareader.reader.engine.layout

import com.mangareader.core.model.EdgeColors

/**
 * Computes the vertical stacking of [FeedItem]s for the webtoon feed with
 * ZERO gap and NO sub-pixel seam (§5.6a).
 *
 * The trap called out explicitly in the spec: if each page's on-screen
 * height is computed independently as `floor(intrinsicH * scale)` (or
 * worse, kept as a float and rounded per-item), rounding can differ by a
 * hair between neighbours and a 1px hairline gap appears — which reads as
 * *worse* than an honest, larger gap because it looks like a rendering
 * bug rather than a deliberate margin.
 *
 * Fix: integer layout with remainder accumulation. We keep the running
 * "ideal" (unrounded) offset as a fraction using a fixed-point
 * accumulator; each item's rounded height is `round(idealBottom) -
 * round(idealTop)`, which guarantees the sum of rounded heights exactly
 * equals the rounded total and no two adjacent items can create a gap or
 * an overlap — this is the same technique used for pixel-perfect
 * timeline/waveform layouts.
 */
object SeamlessLayoutEngine {

    data class LaidOutItem(
        val item: FeedItem,
        val top: Int,
        val bottom: Int,
    ) {
        val heightPx: Int get() = bottom - top
    }

    data class LayoutResult(
        val items: List<LaidOutItem>,
        val totalHeightPx: Int,
    )

    /**
     * @param items feed items in reading order, already carrying an
     * intrinsic (possibly estimated) [FeedItem.Page.layoutHeightPx] /
     * [FeedItem.EndCap.layoutHeightPx] at the target layout width.
     */
    fun layout(items: List<FeedItem>): LayoutResult {
        val laidOut = ArrayList<LaidOutItem>(items.size)
        // Fixed-point accumulator: we track the ideal cumulative height as
        // a Double, but only ever *emit* integers, and every item's height
        // is derived as a difference of two rounded cumulative values —
        // never as a rounding of the item's own height in isolation.
        var idealCumulative = 0.0
        var roundedCumulative = 0
        for (entry in items) {
            val intrinsicHeight = when (entry) {
                is FeedItem.Page -> entry.layoutHeightPx
                is FeedItem.EndCap -> entry.layoutHeightPx
            }
            idealCumulative += intrinsicHeight
            val newRoundedCumulative = Math.round(idealCumulative).toInt()
            val top = roundedCumulative
            val bottom = newRoundedCumulative
            laidOut.add(LaidOutItem(entry, top, bottom))
            roundedCumulative = newRoundedCumulative
        }
        return LayoutResult(laidOut, roundedCumulative)
    }

    /**
     * Background gradient stops for the seam between two adjacent pages
     * (§5.6b): interpolate between the *bottom* edge color of the page
     * above and the *top* edge color of the page below. This is why the
     * background is "a property of the content, not a setting" — for a
     * white/cream webtoon the seam physically disappears; for a page with
     * black edges (some Korean webtoons deliberately use black gutters)
     * black is the visually correct answer, and we get it automatically.
     *
     * Returns null when there's no "below" (e.g. above an EndCap), in
     * which case the caller should just paint [above]'s bottom color
     * solid — no gradient needed for a one-sided edge.
     */
    fun seamGradient(aboveBottom: Int, belowTop: Int): SeamGradient =
        SeamGradient(startColor = aboveBottom, endColor = belowTop)

    data class SeamGradient(val startColor: Int, val endColor: Int)

    /** Convenience: color to paint above the very first page / below the very last. */
    fun endpointColor(edgeColors: EdgeColors, edge: Edge): Int = when (edge) {
        Edge.TOP -> edgeColors.top
        Edge.BOTTOM -> edgeColors.bottom
    }

    enum class Edge { TOP, BOTTOM }
}
