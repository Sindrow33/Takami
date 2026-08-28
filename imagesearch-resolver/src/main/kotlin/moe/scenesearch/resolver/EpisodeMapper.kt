package moe.scenesearch.resolver

import moe.scenesearch.api.EpisodeMatch
import moe.scenesearch.api.SceneIdentity
import moe.scenesearch.api.SourceEntry
import moe.scenesearch.api.SourceEpisode
import kotlin.math.abs

/**
 * Номер серии у trace.moe получается разбором имени файла: бывает пустым,
 * бывает диапазоном, бывает сквозным по всем сезонам. Учитываем все случаи.
 */
object EpisodeMapper {

    private const val EPS = 0.001

    fun map(
        entry: SourceEntry,
        identity: SceneIdentity,
        episodes: List<SourceEpisode> = entry.episodes,
    ): EpisodeMatch? {
        if (episodes.isEmpty()) return null

        val wanted = identity.episode ?: identity.episodeRange?.first
        if (wanted == null) {
            if (episodes.size == 1) return EpisodeMatch(episodes[0], identity.atSec)
            return null
        }

        val direct = find(episodes, wanted.toDouble())
        if (direct != null) return EpisodeMatch(direct, identity.atSec)

        val offset = entry.absoluteOffset
        if (offset != null && offset > 0) {
            val shifted = find(episodes, (wanted - offset).toDouble())
            if (shifted != null) return EpisodeMatch(shifted, identity.atSec)
        }

        if (episodes.any { it.number < 1.0 }) {
            val zeroBased = find(episodes, (wanted - 1).toDouble())
            if (zeroBased != null) return EpisodeMatch(zeroBased, identity.atSec)
        }

        return null
    }

    private fun find(episodes: List<SourceEpisode>, number: Double): SourceEpisode? =
        episodes.firstOrNull { abs(it.number - number) < EPS }
}
