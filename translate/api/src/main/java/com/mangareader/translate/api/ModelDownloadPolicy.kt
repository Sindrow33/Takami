package com.mangareader.translate.api

/**
 * Решения о загрузке модели — чистые функции над числами и флагами.
 *
 * Вынесены из провайдера намеренно: сам провайдер работает с файлами,
 * сетью и `ConnectivityManager`, и проверить его в JVM нельзя. А
 * ошибаются здесь именно решения — «хватит ли места», «можно ли по
 * мобильной сети», — и ошибка каждого из них дорога: недостаток места
 * замечается на 90% загрузки, а мобильный трафик — в счёте за месяц.
 */
object ModelDownloadPolicy {

    /**
     * Запас поверх размера модели.
     *
     * Скачивание идёт во временный файл, который потом переименовывается,
     * так что пик занимает столько же, сколько сама модель. Запас нужен
     * не под копию, а под то, чтобы устройство не осталось с нулём
     * свободного места: система при этом начинает вести себя
     * непредсказуемо задолго до настоящего нуля.
     */
    const val FREE_SPACE_MARGIN_BYTES = 200L * 1024 * 1024

    fun hasEnoughSpace(freeBytes: Long, requiredBytes: Long): Boolean =
        freeBytes >= requiredBytes + FREE_SPACE_MARGIN_BYTES

    /**
     * Что показать пользователю до начала загрузки.
     *
     * Именно до: место и тип сети проверяются заранее, потому что
     * «не хватило места» на девяноста процентах — это выброшенные
     * мегабайты трафика и потраченное время.
     */
    fun evaluate(
        modelsPresent: Boolean,
        freeBytes: Long,
        requiredBytes: Long,
        online: Boolean,
        metered: Boolean,
        architectureSupported: Boolean,
    ): TranslationAvailability = when {
        modelsPresent -> TranslationAvailability.Ready

        !architectureSupported -> TranslationAvailability.DeviceUnsupported(
            "Перевод не поддерживается на этом процессоре",
        )

        !hasEnoughSpace(freeBytes, requiredBytes) -> TranslationAvailability.DeviceUnsupported(
            "Не хватает места: нужно ${formatSize(requiredBytes + FREE_SPACE_MARGIN_BYTES)}",
        )

        !online -> TranslationAvailability.DownloadFailed("Нет подключения к сети")

        else -> TranslationAvailability.NeedsDownload(
            sizeBytes = requiredBytes,
            meteredWarning = metered,
        )
    }

    /**
     * Можно ли начинать загрузку прямо сейчас.
     *
     * Согласие на мобильную сеть спрашивается один раз и передаётся
     * сюда: молча качать по мобильному тарифу нельзя, но и переспрашивать
     * после того, как человек согласился, — тоже.
     */
    fun mayStart(online: Boolean, metered: Boolean, allowMetered: Boolean): Boolean =
        online && (!metered || allowMetered)

    /**
     * Доля загрузки. Неизвестный общий размер даёт null, а не ноль:
     * полоса, вечно стоящая на нуле, выглядит как зависшая загрузка.
     */
    fun progressOf(downloadedBytes: Long, totalBytes: Long): Float? {
        if (totalBytes <= 0L) return null
        return (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> String.format("%.1f ГБ", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> "${bytes / (1024 * 1024)} МБ"
        bytes >= 1024L -> "${bytes / 1024} КБ"
        else -> "$bytes Б"
    }
}
