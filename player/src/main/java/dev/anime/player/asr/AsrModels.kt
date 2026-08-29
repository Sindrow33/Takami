package dev.anime.player.asr

/** Какой нативный backend делает распознавание — см. README TODO (sherpa-onnx / whisper.cpp). */
enum class AsrBackend { SherpaOnnx, WhisperCpp }

/** Сырой результат распознавания одного фрагмента аудио, до сборки в общий [dev.anime.player.subtitle.SubtitleDocument]. */
data class AsrSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val confidence: Float = 1f,
)

/**
 * Источник PCM-аудио эпизода, независимый от видео-рендера [dev.anime.player.core.PlayerEngine]:
 * ASR не может ждать декодер ExoPlayer, ему нужен произвольный оконный доступ к сэмплам —
 * поэтому это отдельный декод-пайплайн (MediaExtractor/FFmpeg), который реализуется выше.
 */
interface AudioSource {
    /** PCM16 mono, ресемплированный до [sampleRateHz] (обычно 16000 для sherpa-onnx/whisper.cpp). */
    suspend fun pcm16(startMs: Long, endMs: Long, sampleRateHz: Int): ShortArray
}

/** Абстракция над конкретной ASR-моделью. [fingerprint] участвует в ключе дискового кэша. */
interface AsrEngine {
    val backend: AsrBackend
    val fingerprint: String
    val sampleRateHz: Int

    /**
     * [offsetMs] — абсолютное начало окна в эпизоде, чтобы движок мог вернуть таймкоды
     * либо относительные (тогда координатор сам добавит offset), либо уже абсолютные —
     * см. [dev.anime.player.asr.normalizeSegments].
     */
    suspend fun transcribe(pcm16: ShortArray, sampleRateHz: Int, offsetMs: Long, language: String?): List<AsrSegment>
}

enum class AsrGenerationStage { Idle, Extracting, Transcribing, Merging, Complete, Error }

data class AsrGenerationProgress(
    val generatedUpToMs: Long,
    val durationMs: Long,
    val stage: AsrGenerationStage,
) {
    val percent: Int
        get() = if (durationMs <= 0L) 0 else ((generatedUpToMs * 100) / durationMs).toInt().coerceIn(0, 100)
}

/**
 * Приводит сегменты движка к абсолютным таймкодам эпизода: часть ASR-движков (в т.ч. типовые
 * sherpa-onnx стриминговые обёртки) отдают время относительно начала переданного окна.
 */
fun normalizeSegments(segments: List<AsrSegment>, windowOffsetMs: Long, timesAreRelative: Boolean): List<AsrSegment> {
    if (!timesAreRelative) return segments
    return segments.map { it.copy(startMs = it.startMs + windowOffsetMs, endMs = it.endMs + windowOffsetMs) }
}
