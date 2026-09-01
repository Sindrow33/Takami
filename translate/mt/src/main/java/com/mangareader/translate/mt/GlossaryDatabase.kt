package com.mangareader.translate.mt

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The one Room database that MUST be a singleton shared by file path
 * across the manga reader and the future light-novel reader (§2.2, §7).
 * `:feature:reader`'s DI module builds this with a fixed, app-wide file
 * name (`glossary.db`) rather than a per-feature-module name so both
 * components' DI graphs converge on the same file regardless of which
 * one initializes it first.
 *
 * Deliberately the ONLY table in this database — translation caches,
 * page metadata, and reader prefs (§8) are per-component concerns that
 * live in `:translate:core`'s own database, not here, precisely so that
 * this shared database stays minimal and never accidentally becomes a
 * coupling surface for anything beyond the glossary.
 */
@Database(entities = [GlossaryEntity::class], version = 1, exportSchema = false)
abstract class GlossaryDatabase : RoomDatabase() {
    abstract fun glossaryDao(): GlossaryDao

    companion object {
        const val FILE_NAME = "glossary.db"

        fun build(context: Context): GlossaryDatabase =
            Room.databaseBuilder(context.applicationContext, GlossaryDatabase::class.java, FILE_NAME)
                .build()
    }
}
