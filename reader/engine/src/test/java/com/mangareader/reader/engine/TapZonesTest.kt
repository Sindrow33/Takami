package com.mangareader.reader.engine

import com.mangareader.reader.engine.gesture.TapAction
import com.mangareader.reader.engine.gesture.TapZoneScheme
import com.mangareader.reader.engine.gesture.TapZones
import com.mangareader.reader.engine.settings.ReaderSettings
import com.mangareader.reader.engine.settings.ReadingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TapZonesTest {

    @Test
    fun `центр экрана открывает меню во всех схемах`() {
        TapZoneScheme.entries.forEach { scheme ->
            assertEquals(
                TapAction.MENU,
                TapZones.resolve(scheme, normalizedX = .5f, normalizedY = .5f, isRtl = false),
                "схема $scheme обязана отдавать центр под меню",
            )
        }
    }

    @Test
    fun `RTL зеркалит стороны листания`() {
        val leftLtr = TapZones.resolve(TapZoneScheme.EDGES, .05f, .5f, isRtl = false)
        val leftRtl = TapZones.resolve(TapZoneScheme.EDGES, .05f, .5f, isRtl = true)

        assertEquals(TapAction.PREVIOUS, leftLtr)
        assertEquals(TapAction.NEXT, leftRtl, "в RTL левый край обязан вести вперёд")
    }

    @Test
    fun `выключенные зоны не листают вообще`() {
        listOf(.02f, .5f, .98f).forEach { x ->
            assertEquals(
                TapAction.MENU,
                TapZones.resolve(TapZoneScheme.DISABLED, x, .5f, isRtl = false),
                "при выключенных зонах любой тап — только меню",
            )
        }
    }

    @Test
    fun `односторонние схемы листают только со своей стороны`() {
        assertEquals(TapAction.NEXT, TapZones.resolve(TapZoneScheme.RIGHT_ONLY, .9f, .5f, isRtl = false))
        assertEquals(TapAction.MENU, TapZones.resolve(TapZoneScheme.RIGHT_ONLY, .1f, .5f, isRtl = false))

        assertEquals(TapAction.PREVIOUS, TapZones.resolve(TapZoneScheme.LEFT_ONLY, .1f, .5f, isRtl = false))
        assertEquals(TapAction.MENU, TapZones.resolve(TapZoneScheme.LEFT_ONLY, .9f, .5f, isRtl = false))
    }
}

class ReaderSettingsTest {

    @Test
    fun `по умолчанию читалка в ленте и без RTL`() {
        val s = ReaderSettings.DEFAULT
        assertEquals(ReadingMode.WEBTOON, s.readingMode)
        assertFalse(s.readingMode.isRtl)
        assertFalse(s.readingMode.isPaged)
    }

    @Test
    fun `разворот на две страницы игнорируется в ленте`() {
        val webtoon = ReaderSettings(readingMode = ReadingMode.WEBTOON, tabletDoubleSpread = true)
        assertFalse(
            webtoon.effectiveDoubleSpread,
            "в непрерывной ленте разворота быть не может, даже если флаг включён",
        )

        val paged = ReaderSettings(readingMode = ReadingMode.PAGED_RTL, tabletDoubleSpread = true)
        assertTrue(paged.effectiveDoubleSpread)
    }

    @Test
    fun `неизвестный режим из базы откатывается к значению по умолчанию`() {
        // Строка из будущей версии приложения не должна ронять читалку.
        assertEquals(ReadingMode.DEFAULT, ReadingMode.fromName("PAGED_VERTICAL_3D"))
        assertEquals(ReadingMode.DEFAULT, ReadingMode.fromName(null))
        assertEquals(ReadingMode.PAGED_RTL, ReadingMode.fromName("PAGED_RTL"))
    }

    @Test
    fun `только RTL-режим считается справа налево`() {
        assertTrue(ReadingMode.PAGED_RTL.isRtl)
        assertFalse(ReadingMode.PAGED_LTR.isRtl)
        assertFalse(ReadingMode.WEBTOON.isRtl)
    }
}
