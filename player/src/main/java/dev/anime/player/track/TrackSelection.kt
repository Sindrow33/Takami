package dev.anime.player.track

/**
 * Выбор дорожки по умолчанию. Чистые функции над списком — поведение
 * «какая озвучка включится сама» иначе проверяется только на устройстве,
 * а ошибка в нём выглядит как «плеер включил не то» без всяких симптомов
 * в логе.
 */
object TrackSelection {

    /**
     * Предпочтительная звуковая дорожка.
     *
     * Порядок: язык из настроек, потом дорожка, помеченная в контейнере как
     * default, потом первая. Дорожку с 5.1 не приоритизируем: на телефоне
     * многоканальный звук сводится в стерео и часто звучит тише диалогов.
     */
    fun preferredAudio(tracks: List<MediaTrack>, preferredLanguages: List<String>): MediaTrack? {
        val audio = tracks.filter { it.kind == TrackKind.Audio }
        if (audio.isEmpty()) return null
        preferredLanguages.forEach { wanted ->
            audio.firstOrNull { matches(it.language, wanted) }?.let { return it }
        }
        return audio.firstOrNull { it.isDefault } ?: audio.first()
    }

    /**
     * Предпочтительные субтитры.
     *
     * Отличие от звука принципиальное: если звук уже на понятном языке,
     * субтитры включать не надо — иначе при русской озвучке поверх видео
     * висят русские субтитры, чего никто не просил. Форсированные при этом
     * оставляем: они переводят надписи, а не речь.
     */
    fun preferredSubtitle(
        tracks: List<MediaTrack>,
        preferredLanguages: List<String>,
        selectedAudioLanguage: String?,
    ): MediaTrack? {
        val subs = tracks.filter { it.kind == TrackKind.Subtitle }
        if (subs.isEmpty()) return null

        val audioUnderstood = preferredLanguages.any { matches(selectedAudioLanguage, it) }
        if (audioUnderstood) {
            return subs.firstOrNull { it.isForced }
        }
        preferredLanguages.forEach { wanted ->
            subs.firstOrNull { matches(it.language, wanted) && !it.isForced }?.let { return it }
        }
        return subs.firstOrNull { it.isDefault }
    }

    /** Сравнение языков с учётом ru/rus/ru-RU. */
    fun matches(language: String?, wanted: String): Boolean {
        val a = normalize(language) ?: return false
        val b = normalize(wanted) ?: return false
        if (a == b) return true
        // Трёхбуквенный код против двухбуквенного: "rus" ~ "ru".
        return a.take(2) == b.take(2) && (a.length == 2 || b.length == 2)
    }

    private fun normalize(code: String?): String? =
        code?.trim()?.lowercase()?.substringBefore('-')?.takeIf { it.isNotEmpty() }

    /** Подписи для списка, с нумерацией безымянных дорожек. */
    fun labels(tracks: List<MediaTrack>): List<String> =
        tracks.mapIndexed { index, track -> track.displayName(index + 1) }
}
