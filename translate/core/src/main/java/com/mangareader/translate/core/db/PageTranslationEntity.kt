package com.mangareader.translate.core.db

import androidx.room.Entity

/**
 * Room row for §8's `page_translation` table: (pageKey + dstLang) -> JSON
 * blob of [com.mangareader.translate.api.TextBlock]s. §4 explicitly calls
 * for storing blocks as one JSON string per page — "a couple of KB per
 * page" — rather than a normalized per-block table, since blocks are
 * always read/written as a whole page unit and never queried
 * individually.
 *
 * Per §8, THIS table is intentionally excluded from the disk-LRU
 * eviction that governs raw page image bytes / inpaint patches: these
 * rows are tiny and precious (they represent real translation work, some
 * of it human-edited) and are only ever cleared explicitly by the user or
 * by a manual "clear translations" action, never automatically by a size
 * cap.
 */
@Entity(tableName = "page_translation", primaryKeys = ["pageKeyRaw", "dstLang"])
data class PageTranslationEntity(
    val pageKeyRaw: String,
    val dstLang: String,
    val srcLang: String,
    /** JSON-encoded `List<TextBlock>` — see [com.mangareader.translate.core.TextBlockJson]. */
    val blocksJson: String,
    val engine: String,
    val createdAt: Long,
)
