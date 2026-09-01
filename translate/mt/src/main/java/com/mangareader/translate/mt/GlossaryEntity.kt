package com.mangareader.translate.mt

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row backing [GlossaryRepositoryImpl]. Keyed by (seriesId, term) so
 * the SAME table, SAME row, is read/written whether the caller is the
 * manga reader or a future light-novel reader — this is the one piece of
 * cross-component shared state the whole project intentionally commits to
 * up front (§2.2, §7): "Kaguya / Kaguya-sama / Kaguya-san" must resolve to
 * one pinned translation across both a manga chapter and a novel chapter
 * of the same title.
 *
 * [seriesId] is intentionally untyped/opaque here — whatever stable
 * per-title id scheme the host app uses to correlate a manga release and
 * a novel release of the "same" work is out of scope for this module; we
 * only require that both callers agree on the string.
 */
@Entity(
    tableName = "glossary",
    primaryKeys = ["seriesId", "term"],
    indices = [Index(value = ["seriesId"])],
)
data class GlossaryEntity(
    val seriesId: String,
    val term: String,
    val translation: String,
    val hits: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
)
