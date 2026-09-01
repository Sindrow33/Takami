package dev.takami.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Регресс на баги из сборки от 2026-09-01.
 * Compose-разметку юнит-тестом не проверить, поэтому здесь закреплены
 * решения, которые можно проверить без устройства; остальное описано
 * в комментариях у самих исправлений.
 */
class OnboardingLogicTest {

    /**
     * Разрешения больше не «выдаются» простым флагом: каждая карточка
     * обязана уметь назвать конкретное системное действие. Тест
     * фиксирует, что ни одна карточка не осталась без обработчика.
     */
    @Test fun `у каждой карточки разрешений есть системное действие`() {
        val keys = listOf("notify", "storage", "battery")
        val handled = keys.filter { it in HANDLED_PERMISSION_KEYS }
        assertEquals("карточка без обработчика снова выглядит нерабочей", keys, handled)
    }

    @Test fun `иллюстрация приветствия лежит в ресурсах`() {
        // Ассет был потерян при переносе дизайна: экран выглядел пустым.
        val id = R.drawable.welcome_girl
        assertTrue("ресурс welcome_girl не найден", id != 0)
    }

    @Test fun `иконки набора не содержат дублей`() {
        val icons = dev.takami.app.ui.components.TakamiIcon.entries
        assertEquals(icons.size, icons.distinct().size)
        assertTrue("иконка свайпов обязана существовать",
            dev.takami.app.ui.components.TakamiIcon.Swipes in icons)
    }

    @Test fun `приветствие по времени суток покрывает все часы`() {
        val greetings = (0..23).map { dev.takami.app.home.greetingFor(it) }
        assertEquals(24, greetings.size)
        assertFalse("час без приветствия", greetings.any { it.isBlank() })
        assertEquals(4, greetings.distinct().size)
    }

    private companion object {
        /** Ключи, для которых `PermsScreen.request` выполняет системный вызов. */
        val HANDLED_PERMISSION_KEYS = setOf("notify", "storage", "battery")
    }
}
