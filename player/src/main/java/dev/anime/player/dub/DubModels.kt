package dev.anime.player.dub

/** Голос TTS для конкретного спикера/дорожки. */
data class TtsVoice(
    val id: String,
    val languageCode: String,
    val gender: VoiceGender = VoiceGender.Unspecified,
)

enum class VoiceGender { Male, Female, Unspecified }

/** Одна реплика на озвучку: текст берётся из субтитров (обычных или ASR, см. [dev.anime.player.asr]). */
data class DubLine(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val speaker: String? = null,
) {
    val originalDurationMs: Long get() = (endMs - startMs).coerceAtLeast(1L)
}

/** Результат синтеза одной реплики: аудио-байты + её реальная длительность (обычно != originalDurationMs). */
data class SynthesizedClip(
    val line: DubLine,
    val audio: ByteArray,
    val mimeType: String,
    val durationMs: Long,
) {
    /** На сколько нужно ускорить/замедлить воспроизведение, чтобы уложиться в тайминг оригинала. */
    val suggestedPlaybackSpeed: Float
        get() = (durationMs.toFloat() / line.originalDurationMs.toFloat())
            .coerceIn(MIN_STRETCH, MAX_STRETCH)

    companion object {
        /** За пределами этого диапазона растяжение речи звучит неразборчиво — лучше обрезать/сдвинуть паузу. */
        const val MIN_STRETCH = 0.85f
        const val MAX_STRETCH = 1.4f
    }
}

enum class DubGenerationStage { Idle, Synthesizing, CacheLookup, Complete, Error }

data class DubGenerationProgress(
    val synthesized: Int,
    val total: Int,
    val stage: DubGenerationStage,
) {
    val percent: Int get() = if (total <= 0) 0 else ((synthesized * 100) / total).coerceIn(0, 100)
}

interface TtsProvider {
    val id: String
    val fingerprint: String

    suspend fun synthesize(text: String, voice: TtsVoice): SynthesisOutput

    /** Раздельный тип, чтобы не тащить [DubLine] в самый низкоуровневый провайдер. */
    data class SynthesisOutput(val audio: ByteArray, val mimeType: String, val durationMs: Long)
}

/** Назначает голос спикеру; по умолчанию — детерминированный round-robin, чтобы один персонаж = один голос. */
interface VoiceMapper {
    fun voiceFor(speaker: String?, languageCode: String): TtsVoice
}

class RoundRobinVoiceMapper(
    private val voices: List<TtsVoice>,
) : VoiceMapper {
    init { require(voices.isNotEmpty()) { "Нужен хотя бы один голос" } }

    private val assigned = LinkedHashMap<String, TtsVoice>()

    override fun voiceFor(speaker: String?, languageCode: String): TtsVoice {
        val key = speaker ?: "__default__"
        return assigned.getOrPut(key) {
            val pool = voices.filter { it.languageCode == languageCode }.ifEmpty { voices }
            pool[assigned.size % pool.size]
        }
    }
}
