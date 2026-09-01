package com.mangareader.reader.engine.gesture

import kotlin.math.max
import kotlin.math.min

/**
 * Pure zoom/pan math shared by paged and webtoon reading modes (§5.6e,
 * §5.8). Framework-agnostic: takes/returns plain floats so it is testable
 * without Android and reusable by both the paged image view and the
 * webtoon feed's per-page zoom.
 *
 * Hard rule from §5.6e: panning while zoomed is clamped so the viewport
 * can NEVER show anything beyond the image's own bounds — there is no
 * value of pan that can produce a black margin. Rubber-banding is
 * explicitly allowed ONLY vertically and ONLY at the true ends of the
 * whole feed (handled one level up, by the feed scroll container, not
 * here); this controller never introduces overscroll on its own.
 */
class ZoomPanController(
    val minScale: Float = 1f,
    val maxScale: Float = 3f, // "pinch-zoom up to 3x" §5.8
) {
    var scale: Float = minScale
        private set
    var panX: Float = 0f
        private set
    var panY: Float = 0f
        private set

    /**
     * Applies an incremental scale change centered at ([focusX], [focusY])
     * in view coordinates, then re-clamps pan so the content can't leave
     * the viewport bounds. [viewportW]/[viewportH]/[contentW]/[contentH]
     * are all in the same unit (px).
     */
    fun onScale(
        scaleFactor: Float,
        focusX: Float,
        focusY: Float,
        viewportW: Float,
        viewportH: Float,
        contentW: Float,
        contentH: Float,
    ) {
        val newScale = (scale * scaleFactor).coerceIn(minScale, maxScale)
        val effectiveFactor = newScale / scale
        // Keep the point under the fingers stationary while scaling.
        panX = focusX - (focusX - panX) * effectiveFactor
        panY = focusY - (focusY - panY) * effectiveFactor
        scale = newScale
        clampPan(viewportW, viewportH, contentW, contentH)
    }

    fun onPan(dx: Float, dy: Float, viewportW: Float, viewportH: Float, contentW: Float, contentH: Float) {
        panX += dx
        panY += dy
        clampPan(viewportW, viewportH, contentW, contentH)
    }

    /** Double-tap: returns to fit-width/fit-screen scale (§5.8) at [minScale]. */
    fun resetToFit() {
        scale = minScale
        panX = 0f
        panY = 0f
    }

    /**
     * The actual clamp. Content dimensions here are *pre-scale* logical
     * size; we compute the scaled content size and forbid any pan that
     * would expose space beyond the content edge on either axis.
     *
     * For the webtoon mode's vertical axis specifically (§5.8: "pan
     * clamped horizontally by image bounds, vertical scroll keeps
     * working"), callers pass [allowVerticalOverflowToParent] = true so
     * this controller does not itself restrict Y — the parent scroll
     * container owns vertical movement instead of this per-page zoom.
     */
    fun clampPan(
        viewportW: Float,
        viewportH: Float,
        contentW: Float,
        contentH: Float,
        allowVerticalOverflowToParent: Boolean = false,
    ) {
        val scaledW = contentW * scale
        val scaledH = contentH * scale

        panX = clampAxis(panX, viewportW, scaledW)
        if (!allowVerticalOverflowToParent) {
            panY = clampAxis(panY, viewportH, scaledH)
        }
    }

    private fun clampAxis(pan: Float, viewport: Float, scaledContent: Float): Float {
        if (scaledContent <= viewport) {
            // Content smaller than viewport on this axis: center it, no pan allowed.
            return (viewport - scaledContent) / 2f
        }
        val minPan = viewport - scaledContent // most negative allowed (content's right/bottom hits viewport edge)
        val maxPan = 0f                        // content's left/top can't go past viewport's left/top
        return min(maxPan, max(minPan, pan))
    }
}
