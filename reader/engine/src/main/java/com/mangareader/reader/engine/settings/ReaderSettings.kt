package com.mangareader.reader.engine.settings

import com.mangareader.reader.engine.gesture.TapZoneScheme

/**
 * Режимы чтения (§5.1). Хранится в `reader_prefs` по seriesId, а не
 * глобально: вебтун и классическая RTL-манга требуют разного, и
 * пользователь не должен переключать это заново на каждом тайтле.
 *
 * `readerPrefs.readingMode` в базе — это [name] значения отсюда;
 * разбор через [fromName], чтобы неизвестная строка из будущей версии
 * не роняла читалку, а откатывалась к значению по умолчанию.
 */
enum class ReadingMode {
    /** Непрерывная вертикальная лента, бесшовно через границы глав. */
    WEBTOON,

    /** Постранично, листание справа налево — классическая манга. */
    PAGED_RTL,

    /** Постранично, слева направо — комиксы и манхва. */
    PAGED_LTR;

    val isPaged: Boolean get() = this != WEBTOON

    /**
     * Направление чтения. Влияет и на листание, и на зеркалирование
     * тап-зон, и на направление слайдера страниц — все три обязаны
     * совпадать, иначе «вперёд» означает разное в разных местах экрана.
     */
    val isRtl: Boolean get() = this == PAGED_RTL

    companion object {
        val DEFAULT = WEBTOON

        fun fromName(name: String?): ReadingMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * Настройки читалки для одного тайтла — то, что лежит в `reader_prefs`.
 *
 * Это не «настройки приложения»: движок ничего не знает про глобальные
 * префы, а хост не обязан иметь UI для каждого поля. Значения по
 * умолчанию подобраны так, чтобы читалка была корректна сразу, без
 * единого сохранённого значения.
 */
data class ReaderSettings(
    val readingMode: ReadingMode = ReadingMode.DEFAULT,
    val tapZoneScheme: TapZoneScheme = TapZoneScheme.L_SHAPE,
    val keepScreenOn: Boolean = true,
    val volumeKeysNavEnabled: Boolean = false,
    /** Обрезка полей страницы. Влияет только на отображение. */
    val cropBordersEnabled: Boolean = false,
    /** Разворот на две страницы в альбомной ориентации; только paged-режимы. */
    val tabletDoubleSpread: Boolean = false,
) {
    /** Разворот имеет смысл только в постраничных режимах. */
    val effectiveDoubleSpread: Boolean get() = tabletDoubleSpread && readingMode.isPaged

    companion object {
        val DEFAULT = ReaderSettings()
    }
}
