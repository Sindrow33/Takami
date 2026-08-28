package moe.scenesearch.resolver

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import moe.scenesearch.api.ContentSource
import moe.scenesearch.api.ExternalIds
import moe.scenesearch.api.MediaKind
import moe.scenesearch.api.ResolvedScene
import moe.scenesearch.api.SceneIdentity
import moe.scenesearch.api.SourceEntry
import moe.scenesearch.api.SourceRegistry

/**
 * Второй этап: глобальный идентификатор превращается в запись конкретного источника.
 * Три уровня по убыванию надёжности: внешний ID, синонимы из оффлайн-базы, нечёткий поиск.
 */
class SourceSceneResolver(
    private val registry: SourceRegistry,
    private val titleIndex: OfflineTitleIndex = OfflineTitleIndex.Empty,
    private val matcher: TitleMatcher = TitleMatcher(),
    private val minConfidence: Double = 0.82,
    private val maxQueriesPerSource: Int = 3,
    private val perSourceTimeoutMillis: Long = 15000,
) {
    suspend fun resolve(
        identity: SceneIdentity,
        kind: MediaKind = MediaKind.ANIME,
    ): List<ResolvedScene> = coroutineScope {
        val references = (titleIndex.expand(identity) + identity.titles.all()).distinct()
        if (references.isEmpty()) return@coroutineScope emptyList()

        val ids = ExternalIds(
            anilist = identity.anilistId,
            mal = identity.malId,
            shikimori = titleIndex.lookup(identity.anilistId, identity.malId)?.shikimoriId,
        )

        val jobs = registry.enabled(kind).map { source ->
            async {
                withTimeoutOrNull(perSourceTimeoutMillis) {
                    runCatching { resolveIn(source, identity, ids, references) }.getOrNull()
                }
            }
        }

        jobs.awaitAll().filterNotNull().sortedByDescending { it.confidence }
    }

    private suspend fun resolveIn(
        source: ContentSource,
        identity: SceneIdentity,
        ids: ExternalIds,
        references: List<String>,
    ): ResolvedScene? {
        if (!ids.isEmpty) {
            val direct = runCatching { source.findByExternalId(ids) }.getOrNull()
            if (direct != null) return build(source, direct, identity, 1.0)
        }

        var bestEntry: SourceEntry? = null
        var bestScore = 0.0

        for (query in references.take(maxQueriesPerSource)) {
            val hits = runCatching { source.search(query) }.getOrElse { emptyList() }
            for (hit in hits) {
                val candidates = ArrayList<String>()
                if (hit.title.isNotBlank()) candidates.add(hit.title)
                for (alt in hit.altTitles) if (alt.isNotBlank()) candidates.add(alt)

                var score = 0.0
                for (c in candidates) {
                    val s = matcher.score(c, references)
                    if (s > score) score = s
                }
                if (score > bestScore) {
                    bestScore = score
                    bestEntry = hit
                }
            }
            if (bestScore >= 0.97) break
        }

        val entry = bestEntry ?: return null
        if (bestScore < minConfidence) return null
        return build(source, entry, identity, bestScore)
    }

    private suspend fun build(
        source: ContentSource,
        entry: SourceEntry,
        identity: SceneIdentity,
        confidence: Double,
    ): ResolvedScene {
        val episodes = runCatching { source.episodes(entry) }.getOrElse { entry.episodes }
        return ResolvedScene(
            entry = entry.copy(episodes = episodes),
            episode = EpisodeMapper.map(entry, identity, episodes),
            confidence = confidence,
            identity = identity,
        )
    }
}
