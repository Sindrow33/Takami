package dev.anime.player.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import dev.anime.player.asr.AudioSource
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Недостающее звено ASR: PCM-аудио эпизода с произвольным оконным доступом.
 *
 * Почему это отдельный пайплайн, а не дорожка из ExoPlayer: ASR нужен доступ
 * к сэмплам ВПЕРЁД текущей позиции (координатор генерирует субтитры с
 * префетчем), а декодер плеера идёт ровно по воспроизведению. Поэтому здесь
 * второй, независимый декод через `MediaExtractor` + `MediaCodec`.
 *
 * **Только для локального файла.** По сетевому потоку дорожку так не достать:
 * `seekTo` по HLS-сегментам и повторное чтение превращаются в перекачивание
 * серии по кругу. Это ограничение не техническая мелочь, а причина, по которой
 * ИИ-субтитры и озвучка включаются только для скачанного.
 */
class MediaExtractorAudioSource(
    private val context: Context,
    private val uri: Uri,
) : AudioSource {

    /**
     * Декодирует окно `[startMs, endMs)` и приводит к моно [sampleRateHz].
     *
     * Возвращает буфер РОВНО ожидаемой длины: декодер отдаёт кадрами и в
     * границу окна не попадает, а координатор ASR считает окна по времени —
     * «сколько получилось» накапливало бы сдвиг таймкодов по ходу серии.
     */
    override suspend fun pcm16(startMs: Long, endMs: Long, sampleRateHz: Int): ShortArray =
        withContext(Dispatchers.IO) {
            val expected = PcmTransforms.samplesFor(endMs - startMs, sampleRateHz)
            if (expected <= 0) return@withContext ShortArray(0)
            val decoded = runCatching { decodeWindow(startMs, endMs, sampleRateHz) }
                .getOrElse { ShortArray(0) }
            PcmTransforms.fitExact(decoded, expected)
        }

    private fun decodeWindow(startMs: Long, endMs: Long, targetRate: Int): ShortArray {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = audioTrackIndex(extractor) ?: return ShortArray(0)
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return ShortArray(0)
            val sourceRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // SEEK_TO_PREVIOUS_SYNC, а не CLOSEST: декодеру нужен опорный кадр
            // перед окном, иначе первые миллисекунды выходят мусором.
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                val raw = drain(codec, extractor, startMs, endMs)
                val mono = PcmTransforms.toMono(raw, channels)
                return PcmTransforms.resample(mono, sourceRate, targetRate)
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun drain(
        codec: MediaCodec,
        extractor: MediaExtractor,
        startMs: Long,
        endMs: Long,
    ): ShortArray {
        val startUs = startMs * 1000L
        val endUs = endMs * 1000L
        val out = ArrayList<Short>()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(TIMEOUT_US)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index)
                    val size = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
                    if (size < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        val presentationUs = extractor.sampleTime
                        codec.queueInputBuffer(index, 0, size, presentationUs, 0)
                        extractor.advance()
                        // Кадры за концом окна уже не нужны — закрываем вход,
                        // иначе декодируем остаток серии на каждое окно.
                        if (presentationUs > endUs) {
                            inputDone = true
                        }
                    }
                }
            }

            val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (outIndex >= 0) {
                if (info.size > 0 && info.presentationTimeUs + FRAME_SLACK_US >= startUs) {
                    val buffer = codec.getOutputBuffer(outIndex)
                    if (buffer != null) {
                        appendShorts(buffer, info, out)
                    }
                }
                codec.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                if (info.presentationTimeUs > endUs) outputDone = true
            } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                // Вход закрыт и выхода больше нет — дальше ждать нечего.
                outputDone = true
            }
        }

        val result = ShortArray(out.size)
        for (i in out.indices) result[i] = out[i]
        return result
    }

    private fun appendShorts(buffer: ByteBuffer, info: MediaCodec.BufferInfo, out: ArrayList<Short>) {
        val shorts = buffer
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        val count = info.size / 2
        for (i in 0 until count) {
            if (i < shorts.limit()) out.add(shorts.get(i))
        }
    }

    private fun audioTrackIndex(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("audio/")) return i
        }
        return null
    }

    private companion object {
        const val TIMEOUT_US = 10_000L

        /**
         * Допуск на кадр: декодер выдаёт целые кадры, и кадр, начавшийся чуть
         * раньше окна, всё равно содержит его первые сэмплы. Без допуска
         * начало каждого окна теряется.
         */
        const val FRAME_SLACK_US = 100_000L
    }
}
