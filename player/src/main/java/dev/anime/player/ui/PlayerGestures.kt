package dev.anime.player.ui

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Арифметика жестов плеера. Чистые функции над числами — специально, чтобы
 * поведение проверялось JVM-тестом: в тестах Android-классы это заглушки,
 * падающие с «Stub!», и логика, написанная поверх `ExoPlayer`/`Window`,
 * непроверяема и потому неизбежно расходится с задуманной.
 *
 * Раскладка зон и знаки соответствуют Tadami/mpvKt: края экрана — перемотка
 * двойным тапом, центр — пауза, вертикальный свайп слева — яркость, справа —
 * громкость.
 */
object PlayerGestures {

    enum class Zone { Left, Center, Right }

    /** Шаг перемотки двойным тапом. */
    const val SEEK_STEP_MS = 10_000L

    /** Сколько держать плашку перемотки после последнего тапа. */
    const val SEEK_LABEL_TIMEOUT_MS = 800L

    /** Скорость при удержании, как в Tadami. */
    const val LONG_PRESS_SPEED = 2f

    /**
     * Зоны как в Tadami: боковые по 2/5 экрана, центр 1/5.
     *
     * Ровно 1/2 на 1/2 не годится: центральная пауза тогда попадает под палец
     * при любом промахе по краю, а на телефоне промах — норма.
     */
    fun zoneAt(x: Float, width: Float): Zone = when {
        width <= 0f -> Zone.Center
        x < width * 0.4f -> Zone.Left
        x > width * 0.6f -> Zone.Right
        else -> Zone.Center
    }

    /**
     * Накопленная перемотка при серии двойных тапов. Знак меняет направление,
     * а не продолжает счёт: тап влево после трёх вправо даёт −10 с, а не +20.
     */
    fun accumulateSeek(currentMs: Long, forward: Boolean): Long {
        val step = if (forward) SEEK_STEP_MS else -SEEK_STEP_MS
        val sameDirection = currentMs == 0L || (currentMs > 0L) == forward
        return if (sameDirection) currentMs + step else step
    }

    /** Куда прыгнуть, с обрезкой по длительности. */
    fun seekTarget(positionMs: Long, deltaMs: Long, durationMs: Long): Long {
        val raw = positionMs + deltaMs
        return if (durationMs > 0L) raw.coerceIn(0L, durationMs) else raw.coerceAtLeast(0L)
    }

    /** Подпись на плашке: «+30 с» / «−10 с». */
    fun seekLabel(deltaMs: Long): String {
        val seconds = abs(deltaMs) / 1000
        val sign = if (deltaMs >= 0) "+" else "-"
        return sign + seconds + " s"
    }

    /**
     * Новое значение вертикального жеста, 0f..1f.
     *
     * Палец вверх увеличивает, поэтому вычитаем: в экранных координатах ось Y
     * растёт вниз. [fullSwipePx] — сколько пикселей соответствует полному
     * диапазону; берём высоту экрана, чтобы жест не зависел от плотности.
     */
    fun verticalValue(original: Float, startY: Float, currentY: Float, fullSwipePx: Float): Float {
        if (fullSwipePx <= 0f) return original.coerceIn(0f, 1f)
        return (original + (startY - currentY) / fullSwipePx).coerceIn(0f, 1f)
    }

    /** Целое значение громкости из доли 0f..1f. */
    fun volumeSteps(fraction: Float, maxSteps: Int): Int =
        (fraction.coerceIn(0f, 1f) * maxSteps).roundToInt().coerceIn(0, maxSteps)

    /** Проценты для плашки. */
    fun percent(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * 100).roundToInt()

    /**
     * Позиция при горизонтальном свайпе-перемотке. Чувствительность как в
     * Tadami (0.15): полный проход по экрану сдвигает примерно на 15 %
     * ширины в миллисекундах позиции, чтобы жест был управляемым на длинной серии.
     */
    fun horizontalSeek(
        startPositionMs: Long,
        startX: Float,
        currentX: Float,
        durationMs: Long,
        msPerPx: Float = 100f,
    ): Long {
        val delta = ((currentX - startX) * msPerPx).toLong()
        return seekTarget(startPositionMs, delta, durationMs)
    }

    /** Пресеты скорости — те же, что в Tadami. */
    val SPEED_PRESETS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    /** Следующий пресет по кругу — для кнопки скорости в панели. */
    fun nextSpeed(current: Float): Float {
        val index = SPEED_PRESETS.indexOfFirst { abs(it - current) < 0.01f }
        return SPEED_PRESETS[(index + 1) % SPEED_PRESETS.size]
    }

    /** Подпись скорости без лишнего нуля: 1x, 1.25x. */
    fun speedLabel(speed: Float): String {
        val rounded = (speed * 100).roundToInt() / 100f
        val text = if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
        return text + "x"
    }
}
