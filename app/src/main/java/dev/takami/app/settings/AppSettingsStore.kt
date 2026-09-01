package dev.takami.app.settings

import android.content.Context

/**
 * Настройки приложения. Отдельно от `TakamiPrefs` (флаги онбординга) и
 * от `reader_prefs` (настройки чтения по тайтлу) — это три разные вещи
 * с разным временем жизни, и складывать их в один файл значило бы
 * получить общий кеш, который нельзя сбросить по частям.
 */
class AppSettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("takami.settings", Context.MODE_PRIVATE)

    var proxyEnabled: Boolean
        get() = sp.getBoolean(KEY_PROXY, false)
        set(value) = sp.edit().putBoolean(KEY_PROXY, value).apply()

    var autoHealEnabled: Boolean
        get() = sp.getBoolean(KEY_AUTOHEAL, true)
        set(value) = sp.edit().putBoolean(KEY_AUTOHEAL, value).apply()

    var wifiOnlyDownloads: Boolean
        get() = sp.getBoolean(KEY_WIFI_ONLY, true)
        set(value) = sp.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    private companion object {
        const val KEY_PROXY = "proxyEnabled"
        const val KEY_AUTOHEAL = "autoHealEnabled"
        const val KEY_WIFI_ONLY = "wifiOnlyDownloads"
    }
}
