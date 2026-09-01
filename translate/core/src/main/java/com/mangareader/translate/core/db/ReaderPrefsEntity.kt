package com.mangareader.translate.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * §8's `reader_prefs` table. Deliberately per-manga, NOT global: reading
 * mode, tap zones, translation mode/engine choice are all things a user
 * may want to differ between a webtoon and a classic RTL manga, or
 * between a title they're studying (OVERLAY) and one they're just
 * enjoying (REPLACE) — see §5.2, §9 ("engine/model/source-language choice
 * belongs in title settings, never in the main app-wide settings UI").
 */
@Entity(tableName = "reader_prefs")
data class ReaderPrefsEntity(
    @PrimaryKey val seriesId: String,
    val readingMode: String, // serialized ReadingMode enum name
    val tapZoneScheme: String,
    val colorFilterArgb: Int?,
    val brightnessOverride: Float?,
    val cropBordersEnabled: Boolean,
    val tabletDoubleSpread: Boolean,
    val volumeKeysNavEnabled: Boolean,
    val fullScreenChapterTransitionEnabled: Boolean, // opt-in, default false, paged modes only (§5.7)
)
