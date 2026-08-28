package moe.scenesearch.api

/** Тип контента, который умеет искать провайдер или источник. */
enum class MediaKind { ANIME, MANGA, RANOBE }

/** Входной запрос: сырые байты картинки плюс подсказки движку. */
class ImageQuery(
    val bytes: ByteArray,
    val mime: String = "image/jpeg",
    val cutBorders: Boolean = true,
    val filterByAnilistId: Int? = null,
    val kind: MediaKind = MediaKind.ANIME,
)

data class TitleSet(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
    val synonyms: List<String> = emptyList(),
) {
    fun all(): List<String> = (listOfNotNull(romaji, english, native) + synonyms)
        .filter { it.isNotBlank() }
        .distinct()

    fun best(): String = romaji ?: english ?: native ?: synonyms.firstOrNull() ?: ""
}

/**
 * Результат этапа идентификации. Ещё не привязан ни к одному источнику:
 * глобальные ID, названия и позиция во времени.
 */
data class SceneIdentity(
    val providerId: String,
    val anilistId: Int? = null,
    val malId: Int? = null,
    val titles: TitleSet = TitleSet(),
    val episode: Int? = null,
    val episodeRange: IntRange? = null,
    val chapter: String? = null,
    val fromSec: Double = 0.0,
    val toSec: Double = 0.0,
    val similarity: Double = 0.0,
    val isAdult: Boolean = false,
    val previewImageUrl: String? = null,
    val previewVideoUrl: String? = null,
    val rawFilename: String? = null,
) {
    /** Середина найденного отрезка — самая надёжная точка для перемотки. */
    val atSec: Double get() = if (toSec > fromSec) (fromSec + toSec) / 2.0 else fromSec
}

data class QuotaInfo(
    val id: String,
    val priority: Int = 0,
    val concurrency: Int = 1,
    val quota: Int = 0,
    val quotaUsed: Int = 0,
) {
    val left: Int get() = (quota - quotaUsed).coerceAtLeast(0)
}
