package dev.takami.app.library

import android.content.Context
import com.mangareader.reader.engine.novel.NovelFont
import com.mangareader.reader.engine.novel.NovelSettings
import com.mangareader.reader.engine.novel.NovelTheme

/**
 * Настройки чтения текста. Глобальные, а не по тайтлу — в отличие от
 * настроек манги: там режим чтения зависит от произведения (вебтун
 * против классической RTL-манги), а размер шрифта зависит от глаз
 * читателя и одинаков везде. Настройка «по тайтлу» здесь заставляла бы
 * заново крутить шрифт на каждой новой книге.
 */
class NovelSettingsStore(context: Context) {

    private val sp = context.getSharedPreferences("takami.novel", Context.MODE_PRIVATE)

    fun load(): NovelSettings {
        val d = NovelSettings.DEFAULT
        return NovelSettings(
            fontSizeSp = sp.getInt(KEY_FONT_SIZE, d.fontSizeSp),
            lineHeightMultiplier = sp.getFloat(KEY_LINE_HEIGHT, d.lineHeightMultiplier),
            horizontalPaddingDp = sp.getInt(KEY_PADDING, d.horizontalPaddingDp),
            // Неизвестное значение из будущей версии не должно ронять
            // читалку — откатываемся к значению по умолчанию.
            theme = NovelTheme.entries.firstOrNull { it.name == sp.getString(KEY_THEME, null) } ?: d.theme,
            fontFamily = NovelFont.entries.firstOrNull { it.name == sp.getString(KEY_FONT, null) } ?: d.fontFamily,
            paged = sp.getBoolean(KEY_PAGED, d.paged),
            justify = sp.getBoolean(KEY_JUSTIFY, d.justify),
        ).let { loaded ->
            // Значения проходят через те же ограничители, что и при
            // изменении: файл настроек мог остаться от версии с другими
            // границами, и нечитаемый размер шрифта из него применялся
            // бы молча.
            loaded.withFontSize(loaded.fontSizeSp)
                .withLineHeight(loaded.lineHeightMultiplier)
                .withPadding(loaded.horizontalPaddingDp)
        }
    }

    fun save(settings: NovelSettings) {
        sp.edit()
            .putInt(KEY_FONT_SIZE, settings.fontSizeSp)
            .putFloat(KEY_LINE_HEIGHT, settings.lineHeightMultiplier)
            .putInt(KEY_PADDING, settings.horizontalPaddingDp)
            .putString(KEY_THEME, settings.theme.name)
            .putString(KEY_FONT, settings.fontFamily.name)
            .putBoolean(KEY_PAGED, settings.paged)
            .putBoolean(KEY_JUSTIFY, settings.justify)
            .apply()
    }

    private companion object {
        const val KEY_FONT_SIZE = "fontSizeSp"
        const val KEY_LINE_HEIGHT = "lineHeight"
        const val KEY_PADDING = "paddingDp"
        const val KEY_THEME = "theme"
        const val KEY_FONT = "font"
        const val KEY_PAGED = "paged"
        const val KEY_JUSTIFY = "justify"
    }
}
