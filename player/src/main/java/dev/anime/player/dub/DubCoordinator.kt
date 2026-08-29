package dev.anime.player.dub

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Фоновая пакетная генерация озвучки с префетчем, симметрично [dev.anime.player.asr.AsrCoordinator]:
 * синтезируем реплики, которые скоро понадобятся ([prefetchAheadMs]), не блокируя воспроизведение
 * ожиданием TTS на каждую строку. Источник текста — любой [SubtitleDocument]-подобный список
 * реплик (обычные субтитры или свежий ASR-документ), поэтому вход — уже готовый List<DubLine>.
 */
class DubCoordinator(
    private val provider: TtsProvider,
    private val voiceMapper: VoiceMapper,
    private val cache: DubDiskCache,
    private val languageCode: String,
    private val prefetchAheadMs: Long = 60_000L,
) {
    private val mutex = Mutex()
    private var lines: List<DubLine> = emptyList()
    private val clipsByIndex = HashMap<Int, SynthesizedClip>()
    private var synthesizedCount = 0

    suspend fun load(lines: List<DubLine>) = mutex.withLock {
        this.lines = lines.sortedBy { it.startMs }
        this.clipsByIndex.clear()
        this.synthesizedCount = 0
    }

    /** Синтезирует все реплики, начинающиеся до `playbackPositionMs + prefetchAheadMs`, которых ещё нет в памяти. */
    suspend fun advanceTo(playbackPositionMs: Long): DubGenerationProgress {
        val target = playbackPositionMs + prefetchAheadMs
        val due = mutex.withLock {
            lines.filter { it.startMs <= target && !clipsByIndex.containsKey(it.index) }
        }

        for (line in due) {
            val voice = voiceMapper.voiceFor(line.speaker, languageCode)
            val key = DubCacheKey(line.text, voice.id, provider.fingerprint)
            val cached = cache.read(key)
            val output = cached?.let { TtsProvider.SynthesisOutput(it.audio, it.mimeType, it.durationMs) }
                ?: provider.synthesize(line.text, voice).also { cache.write(key, it) }

            val clip = SynthesizedClip(line, output.audio, output.mimeType, output.durationMs)
            mutex.withLock {
                clipsByIndex[line.index] = clip
                synthesizedCount = clipsByIndex.size
            }
        }
        return progressSnapshot()
    }

    suspend fun generateAll(): DubGenerationProgress {
        val maxStart = lines.maxOfOrNull { it.startMs } ?: return progressSnapshot()
        return advanceTo(maxStart)
    }

    suspend fun clipFor(lineIndex: Int): SynthesizedClip? = mutex.withLock { clipsByIndex[lineIndex] }

    /** Клип, чей интервал оригинала покрывает [positionMs] — то, что нужно проигрывать прямо сейчас. */
    suspend fun clipAt(positionMs: Long): SynthesizedClip? = mutex.withLock {
        clipsByIndex.values.firstOrNull { positionMs >= it.line.startMs && positionMs < it.line.endMs }
    }

    suspend fun progressSnapshot(): DubGenerationProgress = mutex.withLock {
        DubGenerationProgress(
            synthesized = synthesizedCount,
            total = lines.size,
            stage = if (lines.isNotEmpty() && synthesizedCount >= lines.size) {
                DubGenerationStage.Complete
            } else {
                DubGenerationStage.Synthesizing
            },
        )
    }
}
