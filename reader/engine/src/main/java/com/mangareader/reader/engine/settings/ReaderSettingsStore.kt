package com.mangareader.reader.engine.settings

/**
 * Хранилище настроек чтения по тайтлу.
 *
 * Интерфейс живёт в движке, а реализация — у хоста: движок не должен
 * знать ни про SharedPreferences, ни про Room. Когда появится
 * `reader_prefs` из `:translate:core`, он встанет сюда же, не задев
 * ни `ReaderViewModel`, ни UI.
 */
interface ReaderSettingsStore {

    fun load(seriesId: String): ReaderSettings

    fun save(seriesId: String, settings: ReaderSettings)

    /** Заглушка на случай, когда хост хранилище не предоставил. */
    object None : ReaderSettingsStore {
        override fun load(seriesId: String): ReaderSettings = ReaderSettings.DEFAULT
        override fun save(seriesId: String, settings: ReaderSettings) = Unit
    }
}

/**
 * Позиция чтения. Отдельно от настроек: пишется на каждой странице, а
 * настройки — раз в несколько дней, и складывать их в одну запись
 * значило бы переписывать настройки сотни раз за главу.
 */
interface ReadingPositionStore {

    /** Последняя прочитанная страница главы, или 0. */
    fun lastPage(chapterId: String): Int

    object None : ReadingPositionStore {
        override fun lastPage(chapterId: String): Int = 0
    }
}
