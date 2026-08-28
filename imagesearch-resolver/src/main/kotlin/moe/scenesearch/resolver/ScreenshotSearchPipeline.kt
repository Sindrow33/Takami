package moe.scenesearch.resolver

import moe.scenesearch.api.ImageQuery
import moe.scenesearch.api.MediaKind
import moe.scenesearch.api.ResolvedScene
import moe.scenesearch.api.SceneIdentity
import moe.scenesearch.api.SceneSearchOutcome

data class ScreenshotSearchResult(
    val outcome: SceneSearchOutcome,
    val resolved: List<ResolvedScene> = emptyList(),
) {
    val identity: SceneIdentity? get() = outcome.best
    val bestMatch: ResolvedScene? get() = resolved.firstOrNull()
    val needsManualPick: Boolean get() = identity != null && resolved.isEmpty()
}

/**
 * Склейка двух этапов. Идентификация передаётся лямбдой, поэтому резолвер
 * не зависит от сетевого модуля и тестируется без подмены HTTP.
 */
class ScreenshotSearchPipeline(
    private val identify: suspend (ImageQuery) -> SceneSearchOutcome,
    private val resolver: SourceSceneResolver,
) {
    suspend fun search(query: ImageQuery): ScreenshotSearchResult {
        val outcome = identify(query)
        val best = outcome.best ?: return ScreenshotSearchResult(outcome)
        return ScreenshotSearchResult(outcome, resolver.resolve(best, query.kind))
    }

    /** Тайтл уже известен, добираем только серию по другому кадру. */
    suspend fun searchWithin(
        query: ImageQuery,
        anilistId: Int,
        kind: MediaKind = MediaKind.ANIME,
    ): ScreenshotSearchResult {
        val narrowed = ImageQuery(
            bytes = query.bytes,
            mime = query.mime,
            cutBorders = query.cutBorders,
            filterByAnilistId = anilistId,
            kind = kind,
        )
        return search(narrowed)
    }
}
