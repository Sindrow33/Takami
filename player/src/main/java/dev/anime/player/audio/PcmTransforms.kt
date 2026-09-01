package dev.anime.player.audio

/**
 * Преобразования PCM: сведение каналов и ресемплинг.
 *
 * Чистые функции над массивами чисел — специально, чтобы поведение
 * проверялось JVM-тестом. Ошибка здесь не падает, а тихо портит звук: ASR
 * получает шум вместо речи и возвращает пустой результат, что снаружи
 * выглядит как «распознавание не работает» без единого исключения в логе.
 */
object PcmTransforms {

    /**
     * Сводит перемежённые каналы в моно усреднением.
     *
     * Именно усреднение, а не «взять левый канал»: в аниме центральный
     * диалог часто сведён в оба канала, но при стереоэффектах реплика
     * может быть панорамирована, и один канал теряет часть речи.
     */
    fun toMono(interleaved: ShortArray, channelCount: Int): ShortArray {
        if (channelCount <= 1) return interleaved
        val frames = interleaved.size / channelCount
        val out = ShortArray(frames)
        for (frame in 0 until frames) {
            var sum = 0
            for (channel in 0 until channelCount) {
                sum += interleaved[frame * channelCount + channel]
            }
            out[frame] = (sum / channelCount).toShort()
        }
        return out
    }

    /**
     * Линейный ресемплинг до [targetRate].
     *
     * Линейной интерполяции для ASR достаточно: модели работают на 16 кГц и
     * обучены на телефонном тракте, а качественный полифазный фильтр здесь
     * стоил бы времени и не изменил бы распознавание.
     */
    fun resample(input: ShortArray, sourceRate: Int, targetRate: Int): ShortArray {
        if (sourceRate <= 0 || targetRate <= 0) return ShortArray(0)
        if (sourceRate == targetRate || input.isEmpty()) return input

        val outSize = ((input.size.toLong() * targetRate) / sourceRate).toInt()
        if (outSize <= 0) return ShortArray(0)
        val out = ShortArray(outSize)
        val step = sourceRate.toDouble() / targetRate.toDouble()
        for (i in 0 until outSize) {
            val position = i * step
            val index = position.toInt()
            val frac = position - index
            val a = input[index.coerceAtMost(input.lastIndex)].toDouble()
            val b = input[(index + 1).coerceAtMost(input.lastIndex)].toDouble()
            out[i] = (a + (b - a) * frac).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /** Сколько сэмплов приходится на отрезок времени. */
    fun samplesFor(durationMs: Long, sampleRateHz: Int): Int =
        ((durationMs.coerceAtLeast(0L) * sampleRateHz) / 1000L).toInt()

    /**
     * Обрезает/дополняет буфер до точной длины окна.
     *
     * Декодер отдаёт кадрами и почти никогда не попадает ровно в границу
     * запрошенного окна. Возвращать «сколько получилось» нельзя: координатор
     * ASR считает окна по времени, и накопленная ошибка сдвигает таймкоды
     * субтитров тем сильнее, чем дальше по серии.
     */
    fun fitExact(input: ShortArray, expectedSamples: Int): ShortArray {
        if (expectedSamples <= 0) return ShortArray(0)
        if (input.size == expectedSamples) return input
        val out = ShortArray(expectedSamples)
        input.copyInto(out, endIndex = minOf(input.size, expectedSamples))
        return out
    }

    /**
     * Средняя громкость окна, 0f..1f — по ней видно, есть ли в окне вообще
     * звук. Полностью тихое окно можно не гонять через ASR: это тишина или
     * музыкальная пауза, а вызов модели на нём стоит столько же, сколько на речи.
     */
    fun rms(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (s in samples) {
            val v = s.toDouble() / Short.MAX_VALUE
            sum += v * v
        }
        return Math.sqrt(sum / samples.size).toFloat().coerceIn(0f, 1f)
    }

    /** Ниже этого уровня окно считаем тишиной. */
    const val SILENCE_RMS = 0.005f

    fun isSilence(samples: ShortArray): Boolean = rms(samples) < SILENCE_RMS
}
