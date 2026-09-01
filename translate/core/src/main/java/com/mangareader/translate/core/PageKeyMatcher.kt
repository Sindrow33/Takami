package com.mangareader.translate.core

import com.mangareader.core.model.PageKey
import com.mangareader.translate.core.db.PageMetaDao
import com.mangareader.translate.core.db.PageTranslationDao

/**
 * Implements §3's matching policy end to end against the database: exact
 * [PageKey] hash match first (the fast, common path — same bytes, same
 * hash, single indexed lookup), falling back to Hamming-distance <= 4
 * among rows sharing the exact same pixel dimensions (a cheap proxy for
 * "same aspect ratio" that also happens to be indexable, unlike a float
 * aspect-ratio comparison).
 *
 * This is what makes an online-made translation "just appear" once the
 * same chapter is re-decoded from a downloaded file, or from a different
 * mirror: the NEW decode's [PageKey] either matches exactly, or is close
 * enough in dHash space to the OLD one that this resolves it anyway.
 */
class PageKeyMatcher(
    private val pageMetaDao: PageMetaDao,
    private val pageTranslationDao: PageTranslationDao,
) {
    /** @return the best-matching existing [PageKey] for [candidate], or [candidate] itself if nothing matched. */
    suspend fun resolveCanonicalKey(candidate: PageKey, dstLang: String): PageKey {
        val exact = pageTranslationDao.find(candidate.v, dstLang)
        if (exact != null) return candidate

        val (w, h) = candidate.dimensions
        val sameDims = pageMetaDao.findByExactDimensions(w, h)
        for (row in sameDims) {
            val rowKey = PageKey(row.pageKeyRaw)
            if (PageKey.matches(candidate, rowKey)) {
                val hasTranslation = pageTranslationDao.find(rowKey.v, dstLang) != null
                if (hasTranslation) return rowKey
            }
        }
        return candidate
    }
}
