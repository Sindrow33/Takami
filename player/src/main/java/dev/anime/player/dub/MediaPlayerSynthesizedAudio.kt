package dev.anime.player.dub

import android.media.MediaPlayer
import java.io.File

/**
 * Проигрывает синтезированную реплику поверх видео. Реализация [SynthesizedAudioPlayer],
 * которой не было — из-за чего вся озвучка не могла заработать даже при готовом TTS.
 *
 * Байты пишутся во временный файл, а не подаются потоком: `MediaPlayer` не умеет
 * играть из массива, а держать `AudioTrack` с ручным ресемплингом ради реплики
 * на две секунды — это тот самый код, который потом никто не проверит.
 */
class MediaPlayerSynthesizedAudio(
    private val cacheDir: File,
) : SynthesizedAudioPlayer {

    private var player: MediaPlayer? = null
    private var current: File? = null

    override val isPlaying: Boolean
        get() = runCatching { player?.isPlaying == true }.getOrDefault(false)

    override fun play(clip: SynthesizedClip, playbackSpeed: Float) {
        stop()
        val file = File(cacheDir.apply { mkdirs() }, "dub-" + clip.line.index + ".wav")
        runCatching {
            file.writeBytes(clip.audio)
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            // Скорость приводит реплику к длительности оригинала; диапазон
            // ограничен в SynthesizedClip — за его пределами речь неразборчива.
            runCatching {
                mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
            }
            mp.setOnCompletionListener { stop() }
            mp.start()
            player = mp
            current = file
        }.onFailure {
            runCatching { file.delete() }
        }
    }

    override fun stop() {
        val mp = player
        player = null
        runCatching { if (mp?.isPlaying == true) mp.stop() }
        runCatching { mp?.release() }
        current?.let { runCatching { it.delete() } }
        current = null
    }
}
