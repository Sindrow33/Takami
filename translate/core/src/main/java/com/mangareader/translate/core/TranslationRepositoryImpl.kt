package com.mangareader.translate.core

import com.mangareader.core.model.PageKey
import com.mangareader.translate.api.PageTranslation
import com.mangareader.translate.api.TextBlock
import com.mangareader.translate.api.TranslationRepository
import com.mangareader.translate.core.db.PageTranslationDao
import com.mangareader.translate.core.db.PageTranslationEntity
import kotlinx.coroutines.flow.map

/**
 * Room-backed [TranslationRepository]. Wraps [PageKeyMatcher] so callers
 * of [get]/[observe] transparently benefit from §3's fallback matching
 * without needing to know about it.
 */
class TranslationRepositoryImpl(
    private val dao: PageTranslationDao,
    private val matcher: PageKeyMatcher,
) : TranslationRepository {

    override suspend fun get(pageKey: PageKey, dstLang: String): PageTranslation? {
        val canonical = matcher.resolveCanonicalKey(pageKey, dstLang)
        val entity = dao.find(canonical.v, dstLang) ?: return null
        return toModel(entity)
    }

    override fun observe(pageKey: PageKey, dstLang: String) =
        dao.observe(pageKey.v, dstLang).map { it?.let(::toModel) }

    override suspend fun put(translation: PageTranslation) {
        dao.upsert(
            PageTranslationEntity(
                pageKeyRaw = translation.pageKeyRaw,
                dstLang = translation.dstLang,
                srcLang = translation.srcLang,
                blocksJson = TextBlockJson.encode(translation.blocks),
                engine = translation.engine,
                createdAt = translation.createdAt,
            )
        )
    }

    override suspend fun putEditedBlock(pageKey: PageKey, dstLang: String, block: TextBlock) {
        val existing = dao.find(pageKey.v, dstLang)
        val blocks = existing?.let { TextBlockJson.decode(it.blocksJson) }?.toMutableList() ?: mutableListOf()
        val idx = blocks.indexOfFirst { it.id == block.id }
        val editedBlock = block.copy(edited = true)
        if (idx >= 0) blocks[idx] = editedBlock else blocks.add(editedBlock)
        dao.upsert(
            PageTranslationEntity(
                pageKeyRaw = pageKey.v,
                dstLang = dstLang,
                srcLang = existing?.srcLang ?: "",
                blocksJson = TextBlockJson.encode(blocks),
                engine = existing?.engine ?: "manual-edit",
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
        )
    }

    override suspend fun isTranslated(pageKey: PageKey, dstLang: String): Boolean {
        val canonical = matcher.resolveCanonicalKey(pageKey, dstLang)
        return dao.countFor(canonical.v, dstLang) > 0
    }

    private fun toModel(entity: PageTranslationEntity): PageTranslation = PageTranslation(
        pageKeyRaw = entity.pageKeyRaw,
        srcLang = entity.srcLang,
        dstLang = entity.dstLang,
        blocks = TextBlockJson.decode(entity.blocksJson),
        engine = entity.engine,
        createdAt = entity.createdAt,
    )
}
