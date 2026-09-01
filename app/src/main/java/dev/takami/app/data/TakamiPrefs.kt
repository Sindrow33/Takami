package dev.takami.app.data

import android.content.Context

/**
 * Локальные флаги приложения. В прототипе — localStorage
 * ('takami:onboarded', 'takami:screen'); здесь SharedPreferences,
 * позже переносится на DataStore.
 */
class TakamiPrefs(context: Context) {
    private val sp = context.getSharedPreferences("takami", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean(KEY_ONBOARDED, false)
        set(value) = sp.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var lastScreen: String
        get() = sp.getString(KEY_LAST_SCREEN, "home") ?: "home"
        set(value) = sp.edit().putString(KEY_LAST_SCREEN, value).apply()

    companion object {
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_LAST_SCREEN = "lastScreen"
    }
}
