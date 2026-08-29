package app.takami.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReadEvent(
    val titleId: String,
    val format: Format,
    val chapter: Int,
    val atMs: Long,
    val sourceName: String? = null
)

data class Session(
    val titleId: String,
    val format: Format,
    val from: Int,
    val to: Int,
    val startMs: Long,
    val endMs: Long,
    val sourceName: String?
) {
    val chapters get() = to - from + 1
    val minutes get() = ((endMs - startMs) / 60_000L).coerceAtLeast(1)
}

object ReadingLog {
    const val SESSION_GAP_MS = 6 * 60 * 60 * 1000L

    /** подряд прочитанные главы схлопываются в одну запись истории */
    fun sessions(events: List<ReadEvent>, gapMs: Long = SESSION_GAP_MS): List<Session> {
        val open = LinkedHashMap<Pair<String, Format>, Session>()
        val done = mutableListOf<Session>()
        events.sortedBy { it.atMs }.forEach { e ->
            val key = e.titleId to e.format
            val cur = open[key]
            if (cur != null && e.atMs - cur.endMs <= gapMs) {
                open[key] = cur.copy(
                    from = minOf(cur.from, e.chapter),
                    to = maxOf(cur.to, e.chapter),
                    endMs = e.atMs,
                    sourceName = e.sourceName ?: cur.sourceName
                )
            } else {
                cur?.let { done += it }
                open[key] = Session(e.titleId, e.format, e.chapter, e.chapter, e.atMs, e.atMs, e.sourceName)
            }
        }
        done += open.values
        return done.sortedByDescending { it.endMs }
    }

    fun days(events: List<ReadEvent>, zone: ZoneId = ZoneId.systemDefault()): Set<LocalDate> =
        events.map { Instant.ofEpochMilli(it.atMs).atZone(zone).toLocalDate() }.toSet()
}

data class StreakResult(val days: Int, val freezesUsed: Int)

object Streak {
    /**
     * Пропущенный день не обнуляет серию, пока есть заморозки.
     * Обратного отсчёта и потери прогресса "к полуночи" здесь намеренно нет.
     */
    fun current(days: Set<LocalDate>, today: LocalDate, freezeAllowance: Int = 2): StreakResult {
        var cursor = if (today in days) today else today.minusDays(1)
        var count = 0
        var frozen = 0
        while (true) {
            when {
                cursor in days -> count++
                count > 0 && frozen < freezeAllowance -> frozen++
                else -> return StreakResult(count, frozen)
            }
            cursor = cursor.minusDays(1)
        }
    }
}
