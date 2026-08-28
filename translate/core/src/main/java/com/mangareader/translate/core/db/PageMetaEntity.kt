package com.mangareader.translate.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for §8's `page_meta` table: (pageKey, dimensions, 4 edge
 * colors). Keyed by [pageKeyRaw] ([com.mangareader.core.model.PageKey]),
 * NOT by URL — the whole point of content-addressed caching (§3).
 */
@Entity(tableName = "page_meta")
data class PageMetaEntity(
    @PrimaryKey val pageKeyRaw: String,
    val width: Int,
    val height: Int,
    val edgeTop: Int,
    val edgeBottom: Int,
    val edgeLeft: Int,
    val edgeRight: Int,
)
