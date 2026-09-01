package com.mangareader.translate.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PageMetaDao {
    @Query("SELECT * FROM page_meta WHERE pageKeyRaw = :pageKeyRaw LIMIT 1")
    suspend fun find(pageKeyRaw: String): PageMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PageMetaEntity)

    /**
     * §3's Hamming-distance fallback needs candidates to compare against;
     * exact SQL Hamming distance isn't expressible cheaply, so we narrow
     * by aspect-ratio-compatible dimensions first and let the caller
     * ([com.mangareader.translate.core.PageKeyMatcher]) do the bit-count
     * comparison in Kotlin over this much smaller candidate set.
     */
    @Query("SELECT * FROM page_meta WHERE width = :width AND height = :height")
    suspend fun findByExactDimensions(width: Int, height: Int): List<PageMetaEntity>
}

@Dao
interface PageTranslationDao {
    @Query("SELECT * FROM page_translation WHERE pageKeyRaw = :pageKeyRaw AND dstLang = :dstLang LIMIT 1")
    suspend fun find(pageKeyRaw: String, dstLang: String): PageTranslationEntity?

    @Query("SELECT * FROM page_translation WHERE pageKeyRaw = :pageKeyRaw AND dstLang = :dstLang LIMIT 1")
    fun observe(pageKeyRaw: String, dstLang: String): Flow<PageTranslationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PageTranslationEntity)

    @Query("SELECT COUNT(*) FROM page_translation WHERE pageKeyRaw = :pageKeyRaw AND dstLang = :dstLang")
    suspend fun countFor(pageKeyRaw: String, dstLang: String): Int
}

@Dao
interface ReaderPrefsDao {
    @Query("SELECT * FROM reader_prefs WHERE seriesId = :seriesId LIMIT 1")
    suspend fun find(seriesId: String): ReaderPrefsEntity?

    @Query("SELECT * FROM reader_prefs WHERE seriesId = :seriesId LIMIT 1")
    fun observe(seriesId: String): Flow<ReaderPrefsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReaderPrefsEntity)
}

@Dao
interface TitleTranslationSettingsDao {
    @Query("SELECT * FROM title_translation_settings WHERE seriesId = :seriesId LIMIT 1")
    suspend fun find(seriesId: String): TitleTranslationSettingsEntity?

    @Query("SELECT * FROM title_translation_settings WHERE seriesId = :seriesId LIMIT 1")
    fun observe(seriesId: String): Flow<TitleTranslationSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TitleTranslationSettingsEntity)
}
