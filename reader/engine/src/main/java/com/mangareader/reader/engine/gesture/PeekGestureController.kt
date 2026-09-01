package com.mangareader.reader.engine.gesture

/**
 * Pure state machine for the "peek" gesture (§5.3) — the module's
 * signature feature: a long-press instantly reveals the original art;
 * releasing restores the translation. Deliberately framework-agnostic
 * (no `MotionEvent`, no `View`) so it is trivially unit-testable and so
 * `:reader:ui` is the only place that has to know about Android touch
 * plumbing.
 *
 * Contract (spec §5.3):
 *  - 250ms hold threshold, cancelled if movement exceeds touch slop before
 *    the timer fires.
 *  - Must not fight the webtoon scroll gesture: if the pointer moves
 *    vertically more than [touchSlopPx] before the hold timer elapses,
 *    this is a scroll, not a peek — the caller (View) should let the
 *    scroll proceed and this controller reports [PeekPhase.CANCELLED].
 *  - Works at any zoom level and in any reading mode; this class does not
 *    care about zoom/mode, it only tracks pointer-down/move/up timing and
 *    delegates the "did the user mean to scroll" decision to slop
 *    distance, which is a purely geometric, mode-independent signal.
 *  - Fires a single haptic tick at the moment of activation, exposed as a
 *    [PeekPhase.ACTIVATED] transition the caller performs the actual
 *    `View.performHapticFeedback` for.
 *
 * Usage: the owning View feeds [onPointerDown]/[onPointerMove]/[onPointerUp]
 * from its touch dispatch (typically from a [android.view.GestureDetector]
 * or raw `onTouchEvent`), and a scheduled callback invoking [onHoldTimerFired]
 * after [holdThresholdMs]. The View reacts to [phase] transitions by
 * cross-fading the translation/original layers (120ms alpha animation,
 * §5.3) — that animation lives in `:reader:ui`, not here.
 */
class PeekGestureController(
    private val holdThresholdMs: Long = 250L,
    private val touchSlopPx: Float = 24f,
) {
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downTimeMs: Long = 0L
    private var timerArmed: Boolean = false

    var phase: PeekPhase = PeekPhase.IDLE
        private set

    fun onPointerDown(x: Float, y: Float, timeMs: Long): PeekPhase {
        downX = x
        downY = y
        downTimeMs = timeMs
        timerArmed = true
        phase = PeekPhase.ARMED
        return phase
    }

    /** @return the resulting phase; caller should stop the hold timer if [PeekPhase.CANCELLED]. */
    fun onPointerMove(x: Float, y: Float): PeekPhase {
        if (!timerArmed) return phase
        val dx = x - downX
        val dy = y - downY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        if (distance > touchSlopPx) {
            timerArmed = false
            phase = PeekPhase.CANCELLED
        }
        return phase
    }

    /** Call from the scheduled callback armed at pointer-down time + [holdThresholdMs]. */
    fun onHoldTimerFired(): PeekPhase {
        if (!timerArmed) return phase // already cancelled by movement, or already released
        timerArmed = false
        phase = PeekPhase.ACTIVATED
        return phase
    }

    /** @return the resulting phase; if it was [PeekPhase.ACTIVATED], caller restores translation with the fade. */
    fun onPointerUp(): PeekPhase {
        val wasActivated = phase == PeekPhase.ACTIVATED
        timerArmed = false
        phase = if (wasActivated) PeekPhase.RELEASED else PeekPhase.IDLE
        return phase
    }

    fun onPointerCancel(): PeekPhase {
        timerArmed = false
        phase = PeekPhase.CANCELLED
        return phase
    }

    fun reset() {
        timerArmed = false
        phase = PeekPhase.IDLE
    }
}

enum class PeekPhase {
    /** No pointer down, or gesture fully resolved and reset. */
    IDLE,
    /** Pointer down, hold timer running, waiting to see if it's a tap/scroll/hold. */
    ARMED,
    /** Hold threshold elapsed without exceeding slop: original art is now shown. */
    ACTIVATED,
    /** Pointer lifted after activation: translation should fade back in. */
    RELEASED,
    /** Movement exceeded slop before the timer fired: treat as scroll, not peek. */
    CANCELLED,
}
