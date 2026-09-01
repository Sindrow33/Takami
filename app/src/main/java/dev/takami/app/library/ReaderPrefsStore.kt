package dev.takami.app.library

import android.content.Context
import com.mangareader.reader.engine.gesture.TapZoneScheme
import com.mangareader.reader.engine.settings.ReaderSettings
import com.mangareader.reader.engine.settings.ReaderSettingsStore
import com.mangareader.reader.engine.settings.ReadingMode

/**
 * Настройки чтения по тайтлу на стороне приложения.
 *
 * Движок знает только интерфейс `ReaderSettingsStore`, поэтому переезд
 * на Room-таблицу `reader_prefs` не заденет ни ViewModel, ни UI —
 * поменяется только эта реализация.
 *
 * Каждое поле читается через безопасный разбор: строка из будущей
 * версии приложения не должна ронять читалку на старте.
 */
class ReaderPrefsStore(context: Context) : ReaderSettingsStore {

    private val sp = context.getSharedPreferences("takami.reader.prefs", Context.MODE_PRIVATE)

    override fun load(seriesId: String): ReaderSettings {
        val d = ReaderSettings.DEFAULT
        return ReaderSettings(
            readingMode = ReadingMode.fromName(sp.getString(key(seriesId, MODE), null)),
            tapZoneScheme = tapScheme(sp.getString(key(seriesId, TAP_ZONES), null)),
            keepScreenOn = sp.getBoolean(key(seriesId, KEEP_SCREEN_ON), d.keepScreenOn),
            volumeKeysNavEnabled = sp.getBoolean(key(seriesId, VOLUME_KEYS), d.volumeKeysNavEnabled),
            cropBordersEnabled = sp.getBoolean(key(seriesId, CROP_BORDERS), d.cropBordersEnabled),
            tabletDoubleSpread = sp.getBoolean(key(seriesId, DOUBLE_SPREAD), d.tabletDoubleSpread),
        )
    }

    override fun save(seriesId: String, settings: ReaderSettings) {
        sp.edit()
            .putString(key(seriesId, MODE), settings.readingMode.name)
            .putString(key(seriesId, TAP_ZONES), settings.tapZoneScheme.name)
            .putBoolean(key(seriesId, KEEP_SCREEN_ON), settings.keepScreenOn)
            .putBoolean(key(seriesId, VOLUME_KEYS), settings.volumeKeysNavEnabled)
            .putBoolean(key(seriesId, CROP_BORDERS), settings.cropBordersEnabled)
            .putBoolean(key(seriesId, DOUBLE_SPREAD), settings.tabletDoubleSpread)
            .apply()
    }

    private fun tapScheme(name: String?): TapZoneScheme =
        TapZoneScheme.entries.firstOrNull { it.name == name } ?: ReaderSettings.DEFAULT.tapZoneScheme

    private fun key(seriesId: String, field: String) = "$seriesId:$field"

    private companion object {
        const val MODE = "readingMode"
        const val TAP_ZONES = "tapZoneScheme"
        const val KEEP_SCREEN_ON = "keepScreenOn"
        const val VOLUME_KEYS = "volumeKeysNav"
        const val CROP_BORDERS = "cropBorders"
        const val DOUBLE_SPREAD = "doubleSpread"
    }
}
