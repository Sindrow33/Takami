package dev.takami.app

import dev.takami.app.home.greetingFor
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test fun night() {
        assertEquals("Доброй ночи", greetingFor(0))
        assertEquals("Доброй ночи", greetingFor(4))
    }
    @Test fun morning() {
        assertEquals("Доброе утро", greetingFor(5))
        assertEquals("Доброе утро", greetingFor(11))
    }
    @Test fun afternoon() {
        assertEquals("Добрый день", greetingFor(12))
        assertEquals("Добрый день", greetingFor(17))
    }
    @Test fun evening() {
        assertEquals("Добрый вечер", greetingFor(18))
        assertEquals("Добрый вечер", greetingFor(23))
    }
}
