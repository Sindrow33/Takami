package com.mangareader.translate.mt

import com.mangareader.translate.api.GlossaryEntry
import com.mangareader.translate.api.GlossaryRepository

/**
 * Room-backed [GlossaryRepository]. This is the exact class instance the
 * manga reader's DI graph AND a future light-novel reader's DI graph are
 * expected to bind to the SAME [GlossaryDatabase] file — see
 * INTEGRATION.md "Sharing :translate:mt with the novel reader".
 */
class GlossaryRepositoryImpl(private val dao: GlossaryDao) : GlossaryRepository {

    override suspend fun entriesFor(seriesId: String): List<GlossaryEntry> =
        dao.entriesFor(seriesId).map { GlossaryEntry(term = it.term, translation = it.translation) }

    override suspend fun upsert(seriesId: String, term: String, translation: String) {
        val existing = dao.find(seriesId, term)
        dao.upsert(
            GlossaryEntity(
                seriesId = seriesId,
                term = term,
                translation = translation,
                hits = (existing?.hits ?: 0) + 1,
            )
        )
    }

    override suspend fun recordHit(seriesId: String, term: String) {
        dao.incrementHit(seriesId, term, System.currentTimeMillis())
    }
}
