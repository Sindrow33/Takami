package app.takami.core

import kotlin.math.abs
import kotlin.math.ceil

data class Match(
    val offset: Int,
    val merge: Int,
    val confidence: Double,
    val matched: Int,
    val total: Int
) {
    val reliable get() = confidence >= 0.65
}

/**
 * Источники нумеруют главы по-разному: пролог как 0 или 1, "2 в 1" у переводчиков.
 * Подбираем сдвиг и коэффициент склейки по доле глав, реально существующих в цели.
 */
object ChapterMatcher {

    fun map(chapter: Int, offset: Int, merge: Int): Int =
        ceil(chapter.toDouble() / merge).toInt() + offset

    fun detect(from: SourceInfo, to: SourceInfo): Match {
        val src = from.chapters()
        if (src.isEmpty()) return Match(0, 1, 0.0, 0, 0)

        var best = Match(0, 1, -1.0, 0, src.size)
        for (merge in 1..3) {
            for (offset in -MAX_OFFSET..MAX_OFFSET) {
                val hit = src.count { to.has(map(it, offset, merge)) }
                val p = hit.toDouble() / src.size
                if (p - penalty(offset, merge) > best.confidence - penalty(best.offset, best.merge)) {
                    best = Match(offset, merge, p, hit, src.size)
                }
            }
        }
        return best
    }

    /** прогресс переносим номером; если главы нет — возвращаем ближайшую доступную */
    fun translate(progress: Progress, m: Match, to: SourceInfo): Progress {
        val n = map(progress.chapter, m.offset, m.merge).coerceAtLeast(1)
        val safe = if (to.has(n)) n else to.chapters().lastOrNull { it <= n } ?: n
        return when (progress) {
            is Progress.Paged -> Progress.Paged(safe, 0)
            is Progress.Timed -> Progress.Timed(safe, 0)
            is Progress.Textual -> Progress.Textual(safe, 0f)
        }
    }

    fun unavailable(from: SourceInfo, to: SourceInfo, m: Match): List<Int> =
        from.chapters().filterNot { to.has(map(it, m.offset, m.merge)) }

    private fun penalty(offset: Int, merge: Int) = (merge - 1) * 0.04 + abs(offset) * 0.005
    private const val MAX_OFFSET = 6
}
