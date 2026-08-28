package moe.scenesearch.core

import moe.scenesearch.api.ImageQuery
import moe.scenesearch.api.QuotaInfo
import moe.scenesearch.api.SceneSearchError
import moe.scenesearch.api.SceneSearchOutcome
import moe.scenesearch.api.SceneSearchProvider
import moe.scenesearch.core.cache.CacheKey
import moe.scenesearch.core.cache.SceneSearchCache
import moe.scenesearch.core.hash.Digest
import moe.scenesearch.core.hash.FrameDecoder
import moe.scenesearch.core.hash.PerceptualHash
import moe.scenesearch.core.http.withRetry

/**
 * Фасад модуля: сначала кэш, затем провайдеры по порядку, затем отсев мусора.
 * Всё, что нужно вызывающему коду, это метод identify.
 */
class SceneSearchEngine(
    private val providers: List<SceneSearchProvider>,
    private val cache: SceneSearchCache = SceneSearchCache(),
    private val decoder: FrameDecoder = FrameDecoder.None,
    private val minSimilarity: Double = 0.87,
    private val weakSimilarity: Double = 0.75,
) {
    suspend fun identify(query: ImageQuery): SceneSearchOutcome {
        val key = cacheKey(query)
        val cached = cache.get(key)
        if (cached != null) {
            return SceneSearchOutcome(cached, true, cached.firstOrNull()?.providerId)
        }

        val errors = mutableListOf<SceneSearchError>()
        var weakFallback: SceneSearchOutcome? = null

        for (provider in providers.filter { query.kind in it.supports }) {
            try {
                val raw = withRetry { provider.identify(query) }
                val strong = raw.filter { it.similarity >= minSimilarity }
                if (strong.isNotEmpty()) {
                    cache.put(key, strong)
                    return SceneSearchOutcome(strong, false, provider.id)
                }
                val weak = raw.filter { it.similarity >= weakSimilarity }
                if (weak.isNotEmpty() && weakFallback == null) {
                    weakFallback = SceneSearchOutcome(weak, false, provider.id)
                }
            } catch (e: SceneSearchError) {
                errors.add(e)
            }
        }

        val fallback = weakFallback
        if (fallback != null) return fallback.copy(errors = errors)
        if (errors.isNotEmpty() && errors.all { it is SceneSearchError.QuotaDepleted }) throw errors[0]
        return SceneSearchOutcome(emptyList(), false, null, errors)
    }

    suspend fun quotas(): Map<String, QuotaInfo> {
        val out = LinkedHashMap<String, QuotaInfo>()
        for (p in providers) {
            val q = p.quota()
            if (q != null) out[p.id] = q
        }
        return out
    }

    private fun cacheKey(query: ImageQuery): CacheKey {
        val frame = runCatching { decoder.decode(query.bytes) }.getOrNull()
        return if (frame != null) CacheKey.Perceptual(PerceptualHash.dHash(frame))
        else CacheKey.Exact(Digest.sha256(query.bytes))
    }
}
