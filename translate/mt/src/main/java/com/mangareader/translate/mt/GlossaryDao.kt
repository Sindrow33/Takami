package com.mangareader.translate.mt

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GlossaryDao {

    @Query("SELECT * FROM glossary WHERE seriesId = :seriesId ORDER BY hits DESC")
    suspend fun entriesFor(seriesId: String): List<GlossaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GlossaryEntity)

    @Query("SELECT * FROM glossary WHERE seriesId = :seriesId AND term = :term LIMIT 1")
    suspend fun find(seriesId: String, term: String): GlossaryEntity?

    @Query("UPDATE glossary SET hits = hits + 1, updatedAt = :now WHERE seriesId = :seriesId AND term = :term")
    suspend fun incrementHit(seriesId: String, term: String, now: Long)
}
