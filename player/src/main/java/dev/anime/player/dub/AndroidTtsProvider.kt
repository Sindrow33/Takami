package dev.anime.player.dub

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Озвучка через системный TTS Android.
 *
 * Почему именно системный, а не нейросетевой синтез: нейромодель — это
 * десятки мегабайт в APK на язык, а вес сборки в проекте уже был блокером
 * релиза (186 МБ до разделения по ABI). Системный движок присутствует на
 * устройстве, ничего не весит и говорит на языке пользователя. Качество
 * ниже нейросетевого, но озвучка, которая РАБОТАЕТ, полезнее той, которая
 * написана и не включается — а вся эта ветка кода до сих пор ни разу не
 * выполнялась именно потому, что ждала недостающего звена.
 *
 * Интерфейс [TtsProvider] не меняется, так что замена движка на нейросетевой
 * — это отдельная реализация, а не переделка слоя озвучки.
 */
class AndroidTtsProvider(
    context: Context,
    private val cacheDir: File,
    override val id: String = "android-tts",
) : TtsProvider {

    /**
     * Отпечаток включает язык движка: один и тот же текст, озвученный
     * разными голосами, — разные данные, и общий кэш их путал бы.
     */
    override val fingerprint: String = "android-tts-v1"

    private val counter = AtomicInteger(0)
    private var engine: TextToSpeech? = null

    /** Готовит движок; false означает «озвучка на этом устройстве недоступна». */
    suspend fun prepare(context: Context): Boolean = suspendCancellableCoroutine { cont ->
        val tts = TextToSpeech(context) { status ->
            if (cont.isActive) cont.resume(status == TextToSpeech.SUCCESS)
        }
        engine = tts
        cont.invokeOnCancellation { runCatching { tts.shutdown() } }
    }

    override suspend fun synthesize(text: String, voice: TtsVoice): TtsProvider.SynthesisOutput =
        withContext(Dispatchers.IO) {
            val tts = engine ?: error("TTS не инициализирован: сначала prepare()")
            tts.language = Locale.forLanguageTag(voice.languageCode)

            val target = File(cacheDir.apply { mkdirs() }, "tts-" + counter.incrementAndGet() + ".wav")
            val utteranceId = target.name

            val ok = withTimeoutOrNull(SYNTHESIS_TIMEOUT_MS) {
                awaitSynthesis(tts, text, utteranceId, target)
            } ?: false

            if (!ok || !target.isFile || target.length() <= WAV_HEADER_BYTES) {
                runCatching { target.delete() }
                error("синтез не удался для реплики длиной " + text.length)
            }

            val bytes = target.readBytes()
            runCatching { target.delete() }
            TtsProvider.SynthesisOutput(
                audio = bytes,
                mimeType = "audio/wav",
                durationMs = WavInfo.durationMs(bytes),
            )
        }

    private suspend fun awaitSynthesis(
        tts: TextToSpeech,
        text: String,
        utteranceId: String,
        target: File,
    ): Boolean = suspendCancellableCoroutine { cont ->
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) {
                if (id == utteranceId && cont.isActive) cont.resume(true)
            }

            @Deprecated("Требуется базовым классом", ReplaceWith("onError(id, errorCode)"))
            override fun onError(id: String?) {
                if (id == utteranceId && cont.isActive) cont.resume(false)
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId && cont.isActive) cont.resume(false)
            }
        })
        val result = tts.synthesizeToFile(text, null, target, utteranceId)
        if (result != TextToSpeech.SUCCESS && cont.isActive) cont.resume(false)
    }

    fun release() {
        runCatching { engine?.shutdown() }
        engine = null
    }

    private companion object {
        const val SYNTHESIS_TIMEOUT_MS = 15_000L
        const val WAV_HEADER_BYTES = 44
    }
}

/**
 * Разбор WAV-заголовка. Длительность нужна слою озвучки, чтобы растянуть или
 * сжать реплику под тайминг оригинала ([SynthesizedClip.suggestedPlaybackSpeed]);
 * чистые функции над байтами — чтобы это проверялось тестом, а не на слух.
 */
object WavInfo {

    /** 44-байтный канонический заголовок RIFF/WAVE. */
    const val HEADER_SIZE = 44

    fun sampleRate(bytes: ByteArray): Int = readIntLe(bytes, 24)

    fun channels(bytes: ByteArray): Int = readShortLe(bytes, 22)

    fun bitsPerSample(bytes: ByteArray): Int = readShortLe(bytes, 34)

    fun durationMs(bytes: ByteArray): Long {
        if (bytes.size <= HEADER_SIZE) return 0L
        val rate = sampleRate(bytes)
        val channels = channels(bytes).coerceAtLeast(1)
        val bits = bitsPerSample(bytes).let { if (it <= 0) 16 else it }
        val bytesPerSecond = rate.toLong() * channels * (bits / 8)
        if (bytesPerSecond <= 0L) return 0L
        val dataBytes = (bytes.size - HEADER_SIZE).toLong()
        return (dataBytes * 1000L) / bytesPerSecond
    }

    private fun readIntLe(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 >= bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readShortLe(bytes: ByteArray, offset: Int): Int {
        if (offset + 1 >= bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }
}
