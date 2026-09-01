package dev.anime.player.host

/**
 * Очередь серий внутри тайтла: «следующая серия» и «предыдущая».
 *
 * Чистые функции над списком — иначе поведение на границах (последняя серия,
 * единственная серия) проверяется только вручную, а ошибка тут выглядит как
 * «кнопка не работает» или, хуже, как выход из плеера в конце сезона.
 */
object EpisodeQueue {

    /**
     * Ниже этого остатка серия считается досмотренной, и предлагается
     * следующая. Совпадает с порогом [PlaybackPosition.END_THRESHOLD_MS],
     * чтобы «просмотрено» в списке и автопереход не расходились.
     */
    const val NEXT_PROMPT_MS = PlaybackPosition.END_THRESHOLD_MS

    fun indexOf(episodes: List<AnimeCatalog.Episode>, episodeId: String): Int =
        episodes.indexOfFirst { it.id == episodeId }

    fun next(episodes: List<AnimeCatalog.Episode>, episodeId: String): AnimeCatalog.Episode? {
        val index = indexOf(episodes, episodeId)
        if (index < 0) return null
        return episodes.getOrNull(index + 1)
    }

    fun previous(episodes: List<AnimeCatalog.Episode>, episodeId: String): AnimeCatalog.Episode? {
        val index = indexOf(episodes, episodeId)
        if (index <= 0) return null
        return episodes.getOrNull(index - 1)
    }

    /**
     * Пора ли предлагать следующую серию.
     *
     * Требуется известная длительность: при нулевой durationMs любая позиция
     * формально «близка к концу», и предложение выскакивало бы на первой
     * секунде, пока файл ещё открывается.
     */
    fun shouldPromptNext(positionMs: Long, durationMs: Long, hasNext: Boolean): Boolean {
        if (!hasNext || durationMs <= 0L) return false
        return positionMs >= durationMs - NEXT_PROMPT_MS
    }

    /** Подпись на кнопке перехода. */
    fun nextLabel(next: AnimeCatalog.Episode?): String? {
        val episode = next ?: return null
        return if (episode.number > 0) {
            "Следующая: серия " + episode.number
        } else {
            "Следующая: " + episode.title
        }
    }
}
