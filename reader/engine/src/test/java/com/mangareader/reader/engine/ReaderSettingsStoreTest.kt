package com.mangareader.reader.engine

import com.mangareader.reader.engine.gesture.TapZoneScheme
import com.mangareader.reader.engine.settings.ReaderSettings
import com.mangareader.reader.engine.settings.ReaderSettingsStore
import com.mangareader.reader.engine.settings.ReadingMode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Хранилище в памяти — тот же контракт, что у реализации приложения. */
private class FakeStore : ReaderSettingsStore {
    private val map = HashMap<String, ReaderSettings>()
    var saveCount = 0
        private set

    override fun load(seriesId: String): ReaderSettings = map[seriesId] ?: ReaderSettings.DEFAULT

    override fun save(seriesId: String, settings: ReaderSettings) {
        map[seriesId] = settings
        saveCount++
    }
}

class ReaderSettingsStoreTest {

    @Test
    fun `настройки разных тайтлов не смешиваются`() {
        val store = FakeStore()
        store.save("webtoon-title", ReaderSettings(readingMode = ReadingMode.WEBTOON))
        store.save("classic-manga", ReaderSettings(readingMode = ReadingMode.PAGED_RTL))

        assertEquals(ReadingMode.WEBTOON, store.load("webtoon-title").readingMode)
        assertEquals(ReadingMode.PAGED_RTL, store.load("classic-manga").readingMode)
    }

    @Test
    fun `незнакомый тайтл получает значения по умолчанию, а не пустоту`() {
        val store = FakeStore()
        assertEquals(ReaderSettings.DEFAULT, store.load("никогда-не-открывали"))
    }

    @Test
    fun `заглушка ничего не теряет и ничего не обещает`() {
        val none = ReaderSettingsStore.None
        none.save("x", ReaderSettings(readingMode = ReadingMode.PAGED_LTR))
        // Заглушка сознательно не хранит: настройки живут только сессию.
        assertEquals(ReaderSettings.DEFAULT, none.load("x"))
    }

    @Test
    fun `сохраняются все поля, а не только режим чтения`() {
        val store = FakeStore()
        val custom = ReaderSettings(
            readingMode = ReadingMode.PAGED_LTR,
            tapZoneScheme = TapZoneScheme.EDGES,
            keepScreenOn = false,
            cropBordersEnabled = true,
            tabletDoubleSpread = true,
        )
        store.save("t", custom)
        assertEquals(custom, store.load("t"))
    }
}
