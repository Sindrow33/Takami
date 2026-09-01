package dev.anime.player.host

/**
 * Правила возобновления просмотра. Чистые функции над числами: их можно
 * проверить JVM-тестом, в отличие от кода поверх ExoPlayer.
 */
object PlaybackPosition {

    /** Ближе этого к началу — считаем, что смотреть не начинали. */
    const val MIN_RESUME_MS = 10_000L

    /** Ближе этого к концу — серия досмотрена, следующий запуск начинается с нуля. */
    const val END_THRESHOLD_MS = 20_000L

    /** С какой позиции открывать серию. */
    fun resumeFrom(savedMs: Long, durationMs: Long): Long {
        if (savedMs < MIN_RESUME_MS) return 0L
        if (durationMs > 0L && savedMs >= durationMs - END_THRESHOLD_MS) return 0L
        return savedMs
    }

    /** Стоит ли вообще запоминать эту позицию (не сохраняем начало и конец). */
    fun shouldSave(positionMs: Long, durationMs: Long): Boolean {
        if (positionMs < MIN_RESUME_MS) return false
        if (durationMs > 0L && positionMs >= durationMs - END_THRESHOLD_MS) return false
        return true
    }

    /** Досмотрена ли серия — для отметки в списке. */
    fun isWatched(positionMs: Long, durationMs: Long): Boolean =
        durationMs > 0L && positionMs >= durationMs - END_THRESHOLD_MS

    /** Доля просмотра 0f..1f для полоски в списке серий. */
    fun progressFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /** Подпись под серией: «12:30 из 24:00» либо пусто, если не начинали. */
    fun resumeLabel(positionMs: Long, durationMs: Long, format: (Long) -> String): String = when {
        durationMs > 0L && isWatched(positionMs, durationMs) -> "просмотрено"
        positionMs >= MIN_RESUME_MS && durationMs > 0L ->
            "остановились на " + format(positionMs) + " из " + format(durationMs)
        positionMs >= MIN_RESUME_MS -> "остановились на " + format(positionMs)
        else -> ""
    }
}
