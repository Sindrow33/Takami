package com.mangareader.reader.engine.cache

import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Bounded-concurrency page prefetcher for the reading viewport.
 *
 * Mirrors the priority policy described for translation scheduling in §7
 * but applied to raw page bytes: current page highest priority, then a
 * small forward window, with a concurrency cap so a fast scroll-ahead
 * doesn't saturate the network/IO and starve the page the user is
 * actually looking at.
 */
class PagePrefetcher(
    private val source: MangaPageSource,
    private val scope: CoroutineScope,
    maxParallelDownloads: Int = 3,
    /**
     * Дисковый кеш страниц. Если передан — уже скачанная в прошлой
     * сессии страница не качается повторно, и загрузка вообще не
     * стартует.
     */
    private val diskCache: DiskLruPageCache? = null,
) {
    private val semaphore = Semaphore(maxParallelDownloads)
    private val inFlight = ConcurrentHashMap<String, Job>()
    private val results = ConcurrentHashMap<String, File>()

    /** Cancels any in-flight prefetch for pages not in [keepIds] (scroll moved far away). */
    fun trimTo(keepIds: Set<String>) {
        val toCancel = inFlight.keys.filter { it !in keepIds }
        for (id in toCancel) {
            inFlight.remove(id)?.cancel()
        }
    }

    fun cachedFile(pageId: String): File? = results[pageId]

    /**
     * Requests [page] be loaded with the given [priority] (0 = highest).
     * Returns immediately; observe completion via [cachedFile] polling or
     * by collecting the same [MangaPageSource.open] flow directly for the
     * currently-visible page (callers needing progress UI for the current
     * page should not go through the prefetcher, which is fire-and-forget
     * by design for background pages).
     */
    fun request(page: PageRef, priority: Int = 1) {
        if (results.containsKey(page.id) || inFlight.containsKey(page.id)) return
        val job = scope.launch {
            // Диск проверяется до семафора: попадание в кеш не должно
            // занимать слот параллельной загрузки.
            diskCache?.get(page)?.let { cached ->
                results[page.id] = cached
                inFlight.remove(page.id)
                return@launch
            }
            semaphore.withPermit {
                source.open(page)
                    .catch { /* swallowed: a later on-demand open() on this page will surface the error via UI */ }
                    .collect { load ->
                        if (load is PageLoad.Done) {
                            results[page.id] = load.file
                        }
                    }
            }
            inFlight.remove(page.id)
        }
        inFlight[page.id] = job
    }

    fun cancelAll() {
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
    }
}
