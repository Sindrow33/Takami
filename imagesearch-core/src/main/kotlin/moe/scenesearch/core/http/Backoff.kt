package moe.scenesearch.core.http

import kotlinx.coroutines.delay
import moe.scenesearch.api.SceneSearchError
import java.io.IOException

/**
 * Повторяем только осмысленное: лимит параллелизма и пятисотые.
 * Исчерпанную квоту и битую картинку ретраить бессмысленно.
 */
suspend fun <T> withRetry(
    attempts: Int = 3,
    initialDelayMillis: Long = 800,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var delayMs = initialDelayMillis
    var last: Throwable? = null
    var i = 0
    while (i < attempts) {
        try {
            return block()
        } catch (e: SceneSearchError) {
            last = e
            val retryable = e is SceneSearchError.Concurrency || e is SceneSearchError.Unavailable
            if (!retryable || i == attempts - 1) throw e
        } catch (e: IOException) {
            last = e
            if (i == attempts - 1) throw SceneSearchError.Unavailable("network error", e)
        }
        delay(delayMs)
        delayMs = (delayMs * factor).toLong()
        i++
    }
    throw SceneSearchError.Unavailable("retry exhausted", last)
}
