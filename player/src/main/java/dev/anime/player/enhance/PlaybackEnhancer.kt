package dev.anime.player.enhance

import dev.anime.player.asr.AsrCoordinator
import dev.anime.player.asr.AsrGenerationProgress
import dev.anime.player.core.PlayerEngine
import dev.anime.player.dub.DubCoordinator
import dev.anime.player.dub.DubDuckingController
import dev.anime.player.dub.DubGenerationProgress
import dev.anime.player.dub.DubLine
import dev.anime.player.dub.SynthesizedAudioPlayer
import dev.anime.player.dub.SynthesizedClip
import dev.anime.player.subtitle.SubtitleDocument
import dev.anime.player.subtitle.SubtitleFormat

/**
 * Единая точка входа, которую подключает UI ([dev.anime.player.ui.PlayerScreen]): на каждый тик
 * позиции воспроизведения (см. [PlayerEngine.state]) двигает вперёд ASR-генерацию субтитров,
 * держит озвучку синтезированной на несколько реплик вперёд и переключает громкость оригинала,
 * когда сейчас играет реплика дубляжа.
 *
 * Сам модуль не решает, ASR это или обычные субтитры — источник текста для озвучки передаётся
 * снаружи через [loadDubLines] (обычные субтитры конвертируются через [SubtitleDocument.toDubLines],
 * ASR-документ — через [dubLinesFromAsr]), чтобы можно было озвучивать и готовые субтитры без ASR.
 */
class PlaybackEnhancer(
    private val engine: PlayerEngine,
    private val asr: AsrCoordinator? = null,
    private val dub: DubCoordinator? = null,
    private val ducking: DubDuckingController? = null,
    private val audioPlayer: SynthesizedAudioPlayer? = null,
) {
    private var currentDubClipIndex: Int? = null

    data class State(
        val asrProgress: AsrGenerationProgress? = null,
        val asrDocument: SubtitleDocument? = null,
        val dubProgress: DubGenerationProgress? = null,
    )

    /** Готовит пайплайн под новый эпизод; безопасно звать повторно при переключении серии. */
    suspend fun startEpisode(videoIdentity: String, durationMs: Long, language: String?) {
        asr?.start(videoIdentity, durationMs, language)
        currentDubClipIndex = null
    }

    /** Подаёт готовый текст для озвучки: обычные субтитры ИЛИ то, что уже сгенерировал ASR. */
    suspend fun loadDubLines(lines: List<DubLine>) {
        dub?.load(lines)
    }

    /** Реплики для озвучки из уже готового ASR-документа — удобный мост между модулями. */
    fun dubLinesFromAsr(document: SubtitleDocument): List<DubLine> = document.toDubLines()

    /**
     * Основной тик, вызывается плеером при каждом обновлении позиции (см. `ticker` в `Media3Engine`).
     * [nowMs] — реальное время часов, нужно только для плавного fade в [DubDuckingController].
     */
    suspend fun onTick(positionMs: Long, isPlaying: Boolean, nowMs: Long): State {
        val asrProgress = if (isPlaying) asr?.advanceTo(positionMs) else asr?.progressSnapshot()
        val dubProgress = if (isPlaying) dub?.advanceTo(positionMs) else dub?.progressSnapshot()

        val activeClip = dub?.clipAt(positionMs)
        updateDubPlayback(activeClip, positionMs)
        ducking?.onTick(isDubActive = activeClip != null, nowMs = nowMs)

        return State(
            asrProgress = asrProgress,
            asrDocument = asr?.currentDocument(),
            dubProgress = dubProgress,
        )
    }

    /** Останавливает воспроизведение синтезированного клипа (например, при паузе/перемотке). */
    fun stopDubPlayback() {
        audioPlayer?.stop()
        currentDubClipIndex = null
    }

    private fun updateDubPlayback(activeClip: SynthesizedClip?, positionMs: Long) {
        val player = audioPlayer ?: return
        if (activeClip == null) {
            if (player.isPlaying) player.stop()
            currentDubClipIndex = null
            return
        }
        if (currentDubClipIndex == activeClip.line.index) return // уже играет эта реплика

        player.stop()
        player.play(activeClip, activeClip.suggestedPlaybackSpeed)
        currentDubClipIndex = activeClip.line.index
    }
}

/** Превращает любой [SubtitleDocument] (обычные субтитры или ASR-результат) в реплики для озвучки. */
fun SubtitleDocument.toDubLines(): List<DubLine> =
    cues.map { cue -> DubLine(cue.index, cue.startMs, cue.endMs, cue.text, cue.speaker) }

/** Пример построения документа для показа ASR-субтитров как обычного трека — держим формат единым. */
fun asrDocumentAsVtt(document: SubtitleDocument): SubtitleDocument = document.copy(format = SubtitleFormat.Vtt)
