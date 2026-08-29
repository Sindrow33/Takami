package dev.anime.player.asr

import dev.anime.player.subtitle.SubtitleCue
import dev.anime.player.subtitle.SubtitleDocument
import dev.anime.player.subtitle.SubtitleFormat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Фоновая пакетная генерация ASR-субтитров с префетчем впереди текущей позиции
 * воспроизведения (см. README TODO). Работает окнами фиксированной длины, чтобы:
 *  - не держать весь эпизод в памяти как один PCM-буфер;
 *  - показать первые сабы до готовности всего эпизода;
 *  - уметь опережать позицию плеера на [prefetchAheadMs], а не идти строго по кадру.
 *
 * Не привязан к конкретному [dev.anime.player.core.PlayerEngine] — координатор дёргается
 * снаружи с текущей позицией воспроизведения (см. [advanceTo]), а его прогресс/документ
 * потребляются реактивно UI-слоем (Flow подключается выше, здесь — чистая логика).
 */
class AsrCoordinator(
    private val engine: AsrEngine,
    private val audioSource: AudioSource,
    private val cache: AsrDiskCache,
    private val windowMs: Long = 20_000L,
    private val prefetchAheadMs: Long = 45_000L,
    private val timesAreRelativeToWindow: Boolean = true,
) {
    private val mutex = Mutex()
    private var cues = mutableListOf<SubtitleCue>()
    private var generatedUpToMs = 0L
    private var durationMs = 0L
    private var videoIdentity: String = ""
    private var language: String? = null
    private var cacheKey: AsrCacheKey? = null
    private var finished = false

    /** Сбрасывает состояние и пробует восстановить полный результат из кэша. */
    suspend fun start(videoIdentity: String, durationMs: Long, language: String?): AsrGenerationProgress =
        mutex.withLock {
            this.videoIdentity = videoIdentity
            this.durationMs = durationMs
            this.language = language
            this.cacheKey = AsrCacheKey(videoIdentity, engine.fingerprint, language ?: "auto")
            this.cues = mutableListOf()
            this.generatedUpToMs = 0L
            this.finished = false

            val cached = cache.read(cacheKey!!)
            if (cached != null) {
                cues = cached.cues.toMutableList()
                generatedUpToMs = durationMs
                finished = true
                return@withLock AsrGenerationProgress(durationMs, durationMs, AsrGenerationStage.Complete)
            }
            AsrGenerationProgress(0L, durationMs, AsrGenerationStage.Idle)
        }

    /**
     * Генерирует все окна, недостающие вплоть до `playbackPositionMs + prefetchAheadMs`.
     * Безопасно звать часто (например, из таймера тика плеера) — если префетч уже покрыт,
     * функция возвращает текущий прогресс без работы.
     */
    suspend fun advanceTo(playbackPositionMs: Long): AsrGenerationProgress {
        val target = (playbackPositionMs + prefetchAheadMs).coerceAtMost(durationMs)
        while (true) {
            val (from, to, done) = mutex.withLock {
                if (finished || generatedUpToMs >= target) return@withLock Triple(0L, 0L, true)
                val from = generatedUpToMs
                val to = (from + windowMs).coerceAtMost(durationMs)
                Triple(from, to, false)
            }
            if (done) break

            val pcm = audioSource.pcm16(from, to, engine.sampleRateHz)
            val raw = engine.transcribe(pcm, engine.sampleRateHz, from, language)
            val normalized = normalizeSegments(raw, from, timesAreRelativeToWindow)

            mutex.withLock {
                normalized.forEach { seg ->
                    if (seg.text.isNotBlank()) {
                        cues.add(SubtitleCue(cues.size + 1, seg.startMs, seg.endMs, seg.text))
                    }
                }
                generatedUpToMs = to
                if (generatedUpToMs >= durationMs) finished = true
            }
        }
        return progressSnapshot()
    }

    /** Полная догенерация до конца эпизода (например, для фонового prewarm без плеера). */
    suspend fun generateAll(): AsrGenerationProgress {
        val progress = advanceTo(durationMs)
        if (finished) persistIfNeeded()
        return progress
    }

    suspend fun currentDocument(): SubtitleDocument = mutex.withLock {
        SubtitleDocument(cues.sortedBy { it.startMs }, SubtitleFormat.Vtt)
    }

    suspend fun progressSnapshot(): AsrGenerationProgress = mutex.withLock {
        AsrGenerationProgress(
            generatedUpToMs = generatedUpToMs,
            durationMs = durationMs,
            stage = if (finished) AsrGenerationStage.Complete else AsrGenerationStage.Transcribing,
        )
    }

    private suspend fun persistIfNeeded() {
        val key = cacheKey ?: return
        val doc = currentDocument()
        if (doc.cues.isNotEmpty()) cache.write(key, doc)
    }
}
