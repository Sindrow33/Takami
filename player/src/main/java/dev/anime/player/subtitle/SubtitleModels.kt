package dev.anime.player.subtitle

/**
 * Общая модель субтитров, единая для всех источников: встроенные/внешние треки,
 * результат ASR ([dev.anime.player.asr]) и текст для AI-озвучки ([dev.anime.player.dub]).
 * Формат осознанно минимален (без ASS-тегов) — стилизованные субтитры парсит
 * отдельный слой выше (см. TODO в README про парсер ASS-стилей).
 */
data class SubtitleCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    /** Опциональный спикер/канал — используется дубляжом для выбора голоса. */
    val speaker: String? = null,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

data class SubtitleDocument(
    val cues: List<SubtitleCue>,
    val format: SubtitleFormat = SubtitleFormat.Vtt,
) {
    fun cueAt(ms: Long): SubtitleCue? = cues.firstOrNull { ms >= it.startMs && ms < it.endMs }
}

enum class SubtitleFormat(val extension: String) {
    Srt("srt"),
    Vtt("vtt"),
}
