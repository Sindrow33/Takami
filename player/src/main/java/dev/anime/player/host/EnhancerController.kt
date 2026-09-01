package dev.anime.player.host

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dev.anime.player.audio.MediaExtractorAudioSource
import dev.anime.player.core.PlayerEngine
import dev.anime.player.dub.AndroidTtsProvider
import dev.anime.player.dub.DubCoordinator
import dev.anime.player.dub.DubDiskCache
import dev.anime.player.dub.DubDuckingController
import dev.anime.player.dub.MediaPlayerSynthesizedAudio
import dev.anime.player.dub.RoundRobinVoiceMapper
import dev.anime.player.dub.TtsVoice
import dev.anime.player.enhance.EnhancerAvailability
import dev.anime.player.enhance.PlaybackEnhancer
import dev.anime.player.enhance.toDubLines
import dev.anime.player.subtitle.SubtitleDocument
import dev.anime.player.subtitle.SubtitleParser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Сборка слоя ИИ-функций для одной серии — то самое место, которого не было:
 * `PlaybackEnhancer` был написан целиком и не инстанцировался ниоткуда, потому
 * что у него не хватало реализаций `AudioSource` и `SynthesizedAudioPlayer`.
 *
 * Что здесь включается:
 * - **Озвучка** — реально работает: текст берётся из субтитров рядом с файлом,
 *   синтез системным TTS, приглушение оригинала на время реплики.
 * - **ASR-субтитры** — пайплайн звука готов (`MediaExtractorAudioSource`), но
 *   модели распознавания в проекте нет, поэтому включить нечего. Это честная
 *   граница, а не забытая строка: см. [asrStatus].
 *
 * Всё только для скачанного файла, см. [EnhancerAvailability].
 */
class EnhancerController(
    private val context: Context,
    private val cacheRoot: File,
) {

    data class Ready(
        val enhancer: PlaybackEnhancer,
        val lineCount: Int,
        val ttsProvider: AndroidTtsProvider,
        val audioPlayer: MediaPlayerSynthesizedAudio,
    )

    sealed interface Result {
        data class Available(val ready: Ready) : Result
        data class Unavailable(val reason: String) : Result
    }

    /**
     * Готовит озвучку для серии. Возвращает причину отказа текстом, а не null:
     * пользователь должен понимать, почему кнопка не сработала, иначе это
     * выглядит как поломка.
     */
    suspend fun prepareDubbing(
        engine: PlayerEngine,
        episodeUrl: String,
        episodeFileName: String,
        languageCode: String,
    ): Result {
        EnhancerAvailability.unavailableReason(episodeUrl)?.let {
            return Result.Unavailable(it)
        }

        val document = withContext(Dispatchers.IO) {
            loadSidecarSubtitles(episodeUrl, episodeFileName)
        }
        if (document == null || document.cues.isEmpty()) {
            return Result.Unavailable(
                "Рядом с файлом нет субтитров (.srt или .vtt). " +
                    "Озвучка берёт текст из них: распознавание речи пока не подключено."
            )
        }

        val tts = AndroidTtsProvider(context, File(cacheRoot, "tts"))
        if (!tts.prepare(context)) {
            return Result.Unavailable("Синтез речи недоступен на этом устройстве.")
        }

        val audioPlayer = MediaPlayerSynthesizedAudio(File(cacheRoot, "dub-clips"))
        val coordinator = DubCoordinator(
            provider = tts,
            voiceMapper = RoundRobinVoiceMapper(listOf(TtsVoice("default", languageCode))),
            cache = DubDiskCache(File(cacheRoot, "dub-cache")),
            languageCode = languageCode,
        )
        coordinator.load(document.toDubLines())

        val enhancer = PlaybackEnhancer(
            engine = engine,
            asr = null, // модели распознавания нет — см. asrStatus()
            dub = coordinator,
            ducking = DubDuckingController(engine),
            audioPlayer = audioPlayer,
        )
        return Result.Available(
            Ready(enhancer, document.cues.size, tts, audioPlayer)
        )
    }

    /**
     * Пайплайн звука для ASR готов и его можно проверить: он отдаёт моно-PCM
     * нужной частоты для любого окна локального файла. Не хватает только самой
     * модели распознавания.
     */
    fun audioSourceFor(episodeUrl: String): MediaExtractorAudioSource? =
        if (!EnhancerAvailability.isAvailable(episodeUrl)) null
        else MediaExtractorAudioSource(context, Uri.parse(episodeUrl))

    /** Текст для интерфейса: почему ИИ-субтитров пока нет. */
    fun asrStatus(): String =
        "ИИ-субтитры: звуковой пайплайн готов, не хватает модели распознавания — " +
            "она весит десятки мегабайт и подключается отдельно."

    /**
     * Ищет файл субтитров с тем же именем, что у видео.
     *
     * Работает и для `content://`: в выбранной пользователем папке файл рядом
     * ищется через `DocumentFile`, а не по пути — пути у документа может не быть.
     */
    private fun loadSidecarSubtitles(episodeUrl: String, episodeFileName: String): SubtitleDocument? {
        val candidates = SubtitleParser.sidecarNames(episodeFileName)
        val text = if (episodeUrl.startsWith("content://")) {
            readFromTree(Uri.parse(episodeUrl), candidates)
        } else {
            readFromDisk(episodeUrl, candidates)
        }
        return text?.let { SubtitleParser.parse(it) }
    }

    private fun readFromDisk(episodeUrl: String, candidates: List<String>): String? {
        val path = if (episodeUrl.startsWith("file:")) Uri.parse(episodeUrl).path else episodeUrl
        val dir = path?.let { File(it).parentFile } ?: return null
        return candidates
            .map { File(dir, it) }
            .firstOrNull { it.isFile }
            ?.readText()
    }

    private fun readFromTree(documentUri: Uri, candidates: List<String>): String? {
        // У документа есть родитель только через дерево, поэтому идём от
        // самого документа к его каталогу средствами DocumentFile.
        val doc = DocumentFile.fromSingleUri(context, documentUri) ?: return null
        val parent = doc.parentFile ?: return null
        val match = candidates.firstNotNullOfOrNull { name ->
            parent.findFile(name)?.takeIf { it.isFile }
        } ?: return null
        return runCatching {
            context.contentResolver.openInputStream(match.uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
    }
}
