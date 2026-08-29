package dev.anime.player.dub

/**
 * Проигрывает готовый синтезированный клип озвучки поверх видео. Отдельно от [dev.anime.player.core.PlayerEngine],
 * потому что это независимый звуковой поток (обычно `AudioTrack`/`MediaPlayer` с PCM/AAC из TTS),
 * который должен звучать одновременно с приглушённым (не выключенным) оригиналом — см. [DubDuckingController].
 */
interface SynthesizedAudioPlayer {
    /** Стартует клип с нуля; если что-то уже играет — сначала должно быть остановлено вызывающей стороной. */
    fun play(clip: SynthesizedClip, playbackSpeed: Float)
    fun stop()
    val isPlaying: Boolean
}
