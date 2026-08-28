package com.mangareader.translate.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistence for [com.mangareader.translate.api.TitleTranslationSettings]
 * — the ONE place per §9 where engine/model/source-language/mode choices
 * live, always scoped to a title, never surfaced as an app-wide default.
 */
@Entity(tableName = "title_translation_settings")
data class TitleTranslationSettingsEntity(
    @PrimaryKey val seriesId: String,
    val enabled: Boolean,
    val srcLang: String?,
    val dstLang: String,
    val mode: String, // TranslationMode enum name
    val engineProfile: String, // EngineProfile enum name
    val sfxPolicy: String, // SfxPolicy enum name
)
