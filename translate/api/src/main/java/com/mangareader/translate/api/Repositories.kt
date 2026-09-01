package com.mangareader.translate.api

import com.mangareader.core.model.PageKey
import kotlinx.coroutines.flow.Flow

/**
 * Storage contracts consumed by `:reader:ui` / `:feature:reader` without
 * pulling in Room or any persistence detail — the concrete implementation
 * (backed by Room, §8) lives in `:translate:core`. Declaring the
 * interfaces here (rather than in `:translate:core`) lets `:reader:ui`
 * depend only on `:translate:api`, keeping the persistence engine fully
 * swappable and keeping `:reader:ui` free of a Room dependency.
 */
interface TranslationRepository {

    /** Null if no translation exists yet for this exact/matching [PageKey] + [dstLang] (§3 matching policy). */
    suspend fun get(pageKey: PageKey, dstLang: String): PageTranslation?

    fun observe(pageKey: PageKey, dstLang: String): Flow<PageTranslation?>

    suspend fun put(translation: PageTranslation)

    /** Persists a user edit; MUST set `edited = true` on the affected block and must never be overwritten by automatic re-translation (§5.5). */
    suspend fun putEditedBlock(pageKey: PageKey, dstLang: String, block: TextBlock)

    /** True if this page currently has a *ready* translation cached — drives the slider dot indicator (§5.4). */
    suspend fun isTranslated(pageKey: PageKey, dstLang: String): Boolean
}

interface GlossaryRepository {
    suspend fun entriesFor(seriesId: String): List<GlossaryEntry>
    suspend fun upsert(seriesId: String, term: String, translation: String)
    suspend fun recordHit(seriesId: String, term: String)
}

/** Per-title translation configuration, chosen once, not surfaced per-page (§9). */
data class TitleTranslationSettings(
    val seriesId: String,
    val enabled: Boolean,
    val srcLang: String?,
    val dstLang: String,
    val mode: TranslationMode,
    val engineProfile: EngineProfile,
    val sfxPolicy: SfxPolicy,
)

interface TitleTranslationSettingsRepository {
    suspend fun get(seriesId: String): TitleTranslationSettings?
    fun observe(seriesId: String): Flow<TitleTranslationSettings?>
    suspend fun put(settings: TitleTranslationSettings)
}
