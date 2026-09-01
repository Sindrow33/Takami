package dev.anime.player.track

/**
 * Дорожка звука или субтитров, пригодная для показа в списке выбора.
 *
 * Своя модель, а не тип Media3: выбор дорожки и подпись в списке — это
 * логика, которую нужно проверять тестом, а `Tracks.Group` инстанцировать
 * в JVM-тесте нельзя. Слой Media3 сводится к преобразованию в эту модель
 * и обратно по [groupIndex] / [formatIndex].
 */
data class MediaTrack(
    val groupIndex: Int,
    val formatIndex: Int,
    val kind: TrackKind,
    val language: String?,
    val label: String?,
    val isSelected: Boolean = false,
    val channels: Int = 0,
    val isForced: Boolean = false,
    val isDefault: Boolean = false,
) {
    /**
     * Что показать в списке. Порядок предпочтений: собственная подпись
     * дорожки, потом язык, потом номер — «Дорожка 2» лучше пустой строки,
     * а именно пустые подписи у дорожек в аниме встречаются постоянно.
     */
    fun displayName(fallbackNumber: Int): String {
        val base = label?.takeIf { it.isNotBlank() }
            ?: TrackNaming.languageName(language)
            ?: ("Дорожка " + fallbackNumber)
        val suffixes = buildList {
            if (channels >= 6) add("5.1")
            if (isForced) add("форсированные")
        }
        return if (suffixes.isEmpty()) base else base + " · " + suffixes.joinToString(", ")
    }
}

enum class TrackKind { Audio, Subtitle }

object TrackNaming {

    /**
     * Человекочитаемое имя языка по коду. Только те, что реально встречаются
     * в аниме-раздачах; остальное отдаём кодом как есть — это лучше, чем
     * подписать чужой язык неверно.
     */
    private val NAMES = mapOf(
        "ru" to "Русский",
        "rus" to "Русский",
        "en" to "English",
        "eng" to "English",
        "ja" to "日本語",
        "jpn" to "日本語",
        "uk" to "Українська",
        "ukr" to "Українська",
        "zh" to "中文",
        "zho" to "中文",
        "ko" to "한국어",
        "kor" to "한국어",
    )

    fun languageName(code: String?): String? {
        val normalized = code?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        // Медиа-контейнеры пишут и "ru", и "rus", и "ru-RU".
        val base = normalized.substringBefore('-')
        return NAMES[normalized] ?: NAMES[base] ?: normalized.uppercase()
    }
}
