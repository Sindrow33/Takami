package com.mangareader.reader.engine.gesture

/**
 * Configurable tap-zone schemes (§5.1): "L-shaped layout, edges, right
 * only, left only, disabled". Pure geometry — given a tap point and the
 * viewport size, resolves to an [TapAction]. `:reader:ui` wires this to
 * actual page turning / menu toggling.
 *
 * The "L-shaped" scheme (Mihon's default) reserves a corner-to-edge
 * L-region on each side for previous/next, with the remaining center
 * region toggling the menu — matching the reference reader's feel
 * closely enough that Mihon users don't have to re-learn anything.
 */
enum class TapZoneScheme { L_SHAPE, EDGES, RIGHT_ONLY, LEFT_ONLY, DISABLED }

enum class TapAction { PREVIOUS, NEXT, MENU, NONE }

object TapZones {

    /**
     * @param normalizedX,[normalizedY] tap position in 0..1 of the viewport.
     * @param isRtl when true, PREVIOUS/NEXT sides are mirrored (spec §5.1:
     * "slider expands for RTL" — the tap zones must follow the same
     * mirroring so left/right always means "toward the start"/"toward the
     * end" consistently with reading direction).
     */
    fun resolve(
        scheme: TapZoneScheme,
        normalizedX: Float,
        normalizedY: Float,
        isRtl: Boolean,
    ): TapAction {
        if (scheme == TapZoneScheme.DISABLED) return TapAction.MENU

        val leftAction = if (isRtl) TapAction.NEXT else TapAction.PREVIOUS
        val rightAction = if (isRtl) TapAction.PREVIOUS else TapAction.NEXT

        return when (scheme) {
            TapZoneScheme.L_SHAPE -> resolveLShape(normalizedX, normalizedY, leftAction, rightAction)
            TapZoneScheme.EDGES -> resolveEdges(normalizedX, leftAction, rightAction)
            TapZoneScheme.RIGHT_ONLY -> if (normalizedX > 0.66f) rightAction else TapAction.MENU
            TapZoneScheme.LEFT_ONLY -> if (normalizedX < 0.34f) leftAction else TapAction.MENU
            TapZoneScheme.DISABLED -> TapAction.MENU
        }
    }

    private fun resolveLShape(
        x: Float,
        y: Float,
        leftAction: TapAction,
        rightAction: TapAction,
    ): TapAction {
        // Center rectangle toggles the menu; the surrounding L-shaped band
        // on each side (spanning from the top edge down to the bottom
        // edge, not just a thin vertical strip) triggers page turn.
        //
        // Геометрия сознательно на голых Float, без android.graphics.RectF:
        // класс чисто вычислительный, и зависимость от SDK делала его
        // непроверяемым в JVM-тестах (RectF там — заглушка, бросающая
        // «Stub!»).
        val inCenter = x >= CENTER_LEFT && x <= CENTER_RIGHT && y >= CENTER_TOP && y <= CENTER_BOTTOM
        if (inCenter) return TapAction.MENU
        return if (x < 0.5f) leftAction else rightAction
    }

    private const val CENTER_LEFT = 0.30f
    private const val CENTER_RIGHT = 0.70f
    private const val CENTER_TOP = 0.20f
    private const val CENTER_BOTTOM = 0.80f

    private fun resolveEdges(x: Float, leftAction: TapAction, rightAction: TapAction): TapAction = when {
        x < 0.2f -> leftAction
        x > 0.8f -> rightAction
        else -> TapAction.MENU
    }
}
