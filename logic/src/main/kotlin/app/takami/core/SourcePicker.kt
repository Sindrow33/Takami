package app.takami.core

sealed interface Pick {
    /** выбран лучший живой источник */
    data class Auto(val src: SourceInfo, val best: SourceInfo) : Pick {
        val degraded get() = src.id != best.id
        val lostChapters get() = best.lastChapter - src.lastChapter
    }
    data class Pinned(val src: SourceInfo, val best: SourceInfo) : Pick
    /** закреплённый упал, читаем временно с другого, закрепление не снимаем */
    data class PinnedDown(val src: SourceInfo, val pinned: SourceInfo) : Pick
    data object NoneAvailable : Pick
}

class SourceRegistry(
    private val clock: () -> Long = System::currentTimeMillis,
    private val preferLang: String = "ru"
) {
    private data class Health(val fails: Int, val downUntil: Long)
    private val health = mutableMapOf<String, Health>()

    fun isDown(id: String) = (health[id]?.downUntil ?: 0L) > clock()
    fun cooldownLeftMs(id: String) = maxOf(0L, (health[id]?.downUntil ?: 0L) - clock())

    /** каждая ошибка удваивает паузу: 1, 2, 4, 8 минут... но не больше 6 часов */
    fun reportFailure(id: String) {
        val f = (health[id]?.fails ?: 0) + 1
        val wait = minOf(MAX_COOLDOWN, BASE_COOLDOWN shl minOf(f - 1, 20))
        health[id] = Health(f, clock() + wait)
    }

    fun reportSuccess(id: String) { health.remove(id) }

    /** порядок: максимальный номер главы, затем меньше дыр, затем язык, затем скорость */
    fun rank(list: List<SourceInfo>): List<SourceInfo> = list.sortedWith(
        compareByDescending<SourceInfo> { it.lastChapter }
            .thenBy { it.gaps.size }
            .thenBy { if (it.lang == preferLang) 0 else 1 }
            .thenBy { it.latencyMs }
    )

    fun pick(list: List<SourceInfo>, pinnedId: String? = null): Pick {
        val ranked = rank(list)
        if (ranked.isEmpty()) return Pick.NoneAvailable
        val best = ranked.first()
        val live = ranked.filterNot { isDown(it.id) }
        val pinned = pinnedId?.let { id -> ranked.firstOrNull { it.id == id } }

        return when {
            pinned != null && !isDown(pinned.id) -> Pick.Pinned(pinned, best)
            pinned != null -> live.firstOrNull()
                ?.let { Pick.PinnedDown(it, pinned) } ?: Pick.NoneAvailable
            live.isEmpty() -> Pick.NoneAvailable
            else -> Pick.Auto(live.first(), best)
        }
    }

    private companion object {
        const val BASE_COOLDOWN = 60_000L
        const val MAX_COOLDOWN = 6 * 60 * 60 * 1000L
    }
}
