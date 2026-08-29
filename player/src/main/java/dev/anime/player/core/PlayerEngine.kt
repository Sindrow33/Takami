package dev.anime.player.core

import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val isBuffering: Boolean = true,
    val error: String? = null,
)

/**
 * Абстракция над медиа-ядром. Media3 сейчас, libmpv потом.
 * UI и логика скипа/субтитров про реализацию не знают.
 */
interface PlayerEngine {
    val state: StateFlow<PlaybackState>
    fun load(url: String, headers: Map<String, String> = emptyMap(), startMs: Long = 0L)
    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun setSpeed(speed: Float)
    fun addSubtitleTrack(url: String, language: String, mimeType: String)

    /**
     * Громкость дорожки оригинала, 0f..1f. Нужна для приглушения ("ducking") звука
     * ролика поверх которого играет AI-озвучка — см. [dev.anime.player.dub.DubDuckingController].
     */
    fun setVolume(volume: Float)
    fun release()
}
