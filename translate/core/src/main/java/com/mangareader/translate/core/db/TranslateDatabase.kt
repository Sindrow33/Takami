package com.mangareader.translate.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * §8's database: `page_meta`, `page_translation`, `reader_prefs`,
 * `title_translation_settings`. Deliberately separate from
 * `:translate:mt`'s `glossary.db` (see that module's `GlossaryDatabase`
 * kdoc) — the glossary is the one thing shared with the light-novel
 * reader; everything in THIS database is manga-reader-only.
 */
@Database(
    entities = [
        PageMetaEntity::class,
        PageTranslationEntity::class,
        ReaderPrefsEntity::class,
        TitleTranslationSettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TranslateDatabase : RoomDatabase() {
    abstract fun pageMetaDao(): PageMetaDao
    abstract fun pageTranslationDao(): PageTranslationDao
    abstract fun readerPrefsDao(): ReaderPrefsDao
    abstract fun titleTranslationSettingsDao(): TitleTranslationSettingsDao

    companion object {
        const val FILE_NAME = "translate_core.db"

        fun build(context: Context): TranslateDatabase =
            Room.databaseBuilder(context.applicationContext, TranslateDatabase::class.java, FILE_NAME)
                .build()
    }
}
