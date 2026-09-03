package com.mangareader.translate.api

/**
 * Доступен ли перевод прямо сейчас, и если нет — почему.
 *
 * Существует ровно потому, что «недоступно» обязано быть видимым
 * состоянием, а не отключённой кнопкой. Кнопка, которая молча не
 * работает, заставляет человека гадать, что он делает не так;
 * состояние с причиной отвечает на этот вопрос само.
 *
 * Каждая ветка несёт то, что нужно показать пользователю И то, что он
 * может с этим сделать: без второго это просто более подробный отказ.
 */
sealed interface TranslationAvailability {

    /** Всё на месте, перевод работает. */
    data object Ready : TranslationAvailability

    /**
     * Модели нет на устройстве и её можно скачать.
     *
     * @param sizeBytes размер загрузки. Показывается всегда: человек
     *   решает, качать ли сотню мегабайт, до начала, а не после.
     * @param meteredWarning загрузка пойдёт по мобильной сети.
     */
    data class NeedsDownload(
        val sizeBytes: Long,
        val meteredWarning: Boolean,
    ) : TranslationAvailability

    /** Загрузка идёт. [progress] в долях 0..1, null — размер неизвестен. */
    data class Downloading(val progress: Float?) : TranslationAvailability

    /**
     * Скачать не удалось. Причина показывается человеку, поэтому это
     * текст, а не код ошибки.
     */
    data class DownloadFailed(val reason: String) : TranslationAvailability

    /**
     * Модель для этого языка не поставляется вовсе.
     *
     * Отличается от [NeedsDownload] принципиально: там кнопка «скачать»
     * уместна, здесь она вела бы в никуда.
     */
    data class Unsupported(val language: String?) : TranslationAvailability

    /**
     * Устройство не тянет: нет поддерживаемой архитектуры или не хватает
     * места. Скачивание не поможет, и предлагать его нечестно.
     */
    data class DeviceUnsupported(val reason: String) : TranslationAvailability

    /** Можно ли из этого состояния начать загрузку. */
    val canDownload: Boolean
        get() = this is NeedsDownload || this is DownloadFailed

    /** Работает ли перевод. Единственная проверка, нужная читалке. */
    val isReady: Boolean get() = this is Ready
}
