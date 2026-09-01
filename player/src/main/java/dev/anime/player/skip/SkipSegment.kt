package dev.anime.player.skip

enum class SkipType { OP, ED, RECAP, PREVIEW }

data class SkipSegment(
    val type: SkipType,
    val startMs: Long,
    val endMs: Long,
) {
    fun contains(ms: Long): Boolean = ms >= startMs && ms < endMs

    val label: String
        get() = when (type) {
            SkipType.OP -> "Пропустить опенинг"
            SkipType.ED -> "Пропустить эндинг"
            SkipType.RECAP -> "Пропустить рекап"
            SkipType.PREVIEW -> "Пропустить анонс"
        }
}

/**
 * Сегмент, который надо предложить пропустить на позиции [ms].
 *
 * AniSkip отдаёт перекрывающиеся сегменты (типовой случай: рекап 0:00-0:30 и
 * опенинг 0:12-1:42). Наивный `firstOrNull` брал рекап и перематывал на 0:30 —
 * то есть в середину опенинга, и кнопка появлялась второй раз. Берём сегмент с
 * самым дальним концом: один тап уводит за все перекрытые сегменты сразу.
 */
fun activeSegmentAt(segments: List<SkipSegment>, ms: Long): SkipSegment? =
    segments.filter { it.contains(ms) }.maxByOrNull { it.endMs }

interface SkipProvider {
    val name: String
    suspend fun segments(malId: Int?, episode: Int, durationMs: Long): List<SkipSegment>
}

/** Заглушка: проверить UI и авто-скип без сети. */
class FakeSkipProvider : SkipProvider {
    override val name = "fake"
    override suspend fun segments(malId: Int?, episode: Int, durationMs: Long): List<SkipSegment> {
        val out = mutableListOf(SkipSegment(SkipType.OP, 12000L, 102000L))
        if (durationMs > 120000L) {
            out += SkipSegment(SkipType.ED, durationMs - 95000L, durationMs)
        }
        return out
    }
}

/** Первый провайдер с непустым результатом побеждает. */
class ChainedSkipProvider(private val providers: List<SkipProvider>) : SkipProvider {
    override val name = "chain"
    override suspend fun segments(malId: Int?, episode: Int, durationMs: Long): List<SkipSegment> {
        for (p in providers) {
            val r = runCatching { p.segments(malId, episode, durationMs) }.getOrNull()
            if (r != null && r.isNotEmpty()) return r
        }
        return emptyList()
    }
}
