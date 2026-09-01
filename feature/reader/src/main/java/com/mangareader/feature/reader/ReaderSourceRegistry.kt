package com.mangareader.feature.reader

import com.mangareader.core.model.ChapterInfo
import com.mangareader.core.model.MangaPageSource

/**
 * Минимальный DI-заменитель на время интеграции: хост регистрирует
 * реализацию [MangaPageSource] под своим `sourceId`, читалка её достаёт по
 * тому же ключу из [ReaderActivity].
 *
 * Полноценный граф (Hilt/Koin) — отдельная задача (INTEGRATION.md §7);
 * этот объект намеренно не пытается им притворяться: он process-scoped и
 * знает ровно то, что нужно, чтобы открыть главу.
 */
object ReaderSourceRegistry {

    /** Общая шина событий; её же отдаёт [MangaReaderImpl.events]. */
    val eventBus: ReaderEventBus = ReaderEventBus()

    val reader: MangaReaderHandle by lazy { MangaReaderHandle(MangaReaderImpl(eventBus)) }

    private val sources = LinkedHashMap<String, MangaPageSource>()
    private val chapterLookups = LinkedHashMap<String, suspend (String) -> ChapterInfo>()

    fun register(
        sourceId: String,
        source: MangaPageSource,
        chapterLookup: suspend (String) -> ChapterInfo,
    ) {
        sources[sourceId] = source
        chapterLookups[sourceId] = chapterLookup
    }

    fun source(sourceId: String): MangaPageSource? = sources[sourceId]

    fun chapterLookup(sourceId: String): (suspend (String) -> ChapterInfo)? = chapterLookups[sourceId]

    fun isRegistered(sourceId: String): Boolean = sources.containsKey(sourceId)
}

/** Обёртка, чтобы хост не зависел от конкретного класса реализации. */
class MangaReaderHandle(private val impl: MangaReaderImpl) : com.mangareader.core.model.MangaReader by impl
