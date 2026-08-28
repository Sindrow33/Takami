package moe.scenesearch.core.cache

import moe.scenesearch.api.SceneIdentity
import moe.scenesearch.core.hash.PerceptualHash

sealed class CacheKey {
    data class Perceptual(val hash: Long) : CacheKey()
    data class Exact(val digest: String) : CacheKey()
}

/**
 * Один и тот же скрин гуляет по чатам, а квота общая на IP.
 * Похожие картинки с расстоянием Хэмминга не больше порога считаем одним запросом.
 */
class SceneSearchCache(
    private val ttlMillis: Long = 24L * 60 * 60 * 1000,
    private val maxEntries: Int = 256,
    private val hammingThreshold: Int = 5,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class Entry(val key: CacheKey, val value: List<SceneIdentity>, val createdAt: Long)

    private val entries = ArrayDeque<Entry>()
    private val lock = Any()

    fun get(key: CacheKey): List<SceneIdentity>? = synchronized(lock) {
        purge()
        val hit = entries.firstOrNull { matches(it.key, key) } ?: return null
        entries.remove(hit)
        entries.addFirst(hit)
        return hit.value
    }

    fun put(key: CacheKey, value: List<SceneIdentity>) = synchronized(lock) {
        if (value.isEmpty()) return
        entries.removeAll { matches(it.key, key) }
        entries.addFirst(Entry(key, value, clock()))
        while (entries.size > maxEntries) entries.removeLast()
    }

    fun clear() = synchronized(lock) { entries.clear() }

    fun size(): Int = synchronized(lock) {
        purge()
        return entries.size
    }

    private fun matches(a: CacheKey, b: CacheKey): Boolean = when {
        a is CacheKey.Exact && b is CacheKey.Exact -> a.digest == b.digest
        a is CacheKey.Perceptual && b is CacheKey.Perceptual ->
            PerceptualHash.hamming(a.hash, b.hash) <= hammingThreshold
        else -> false
    }

    private fun purge() {
        val now = clock()
        entries.removeAll { now - it.createdAt > ttlMillis }
    }
}
