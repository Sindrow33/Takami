package dev.takami.app

import dev.takami.app.library.NovelLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovelLibraryTest {

    @Test
    fun `пустая строка разделяет абзацы, одиночный перевод — нет`() {
        // Файлы, свёрстанные по 80 колонок, переносят строки внутри
        // абзаца: считать каждый перевод границей значит порвать
        // каждое предложение на куски.
        val text = "Первая строка\nпродолжение той же мысли.\n\nВторой абзац."
        assertEquals(
            listOf("Первая строка продолжение той же мысли.", "Второй абзац."),
            NovelLibrary.splitParagraphs(text),
        )
    }

    @Test
    fun `подряд идущие пустые строки не дают пустых абзацев`() {
        val text = "Абзац.\n\n\n\nВторой."
        assertEquals(listOf("Абзац.", "Второй."), NovelLibrary.splitParagraphs(text))
    }

    @Test
    fun `переводы строк Windows разбираются так же`() {
        assertEquals(
            listOf("Первый.", "Второй."),
            NovelLibrary.splitParagraphs("Первый.\r\n\r\nВторой."),
        )
    }

    @Test
    fun `пустой файл не даёт ни одного абзаца`() {
        assertEquals(emptyList<String>(), NovelLibrary.splitParagraphs("\n\n   \n"))
    }

    @Test
    fun `текстовые главы узнаются по расширению в любом регистре`() {
        assertTrue(NovelLibrary.isTextChapter("Глава 1.TXT"))
        assertTrue(NovelLibrary.isTextChapter("ch1.md"))
        assertFalse(NovelLibrary.isTextChapter("0001.jpg"))
        assertFalse(NovelLibrary.isTextChapter("ch1"))
    }

    @Test
    fun `корректный UTF-8 распознаётся`() {
        assertTrue(NovelLibrary.isValidUtf8("Русский текст".toByteArray(Charsets.UTF_8)))
        assertTrue(NovelLibrary.isValidUtf8("plain ascii".toByteArray()))
    }

    @Test
    fun `текст в windows-1251 не принимается за UTF-8`() {
        // Иначе глава «открывается», но состоит из знаков вопроса —
        // формально успех, фактически читать нечего.
        val cp1251 = "Русский".toByteArray(charset("windows-1251"))
        assertFalse(NovelLibrary.isValidUtf8(cp1251))
    }

    @Test
    fun `обрезанный на границе буфера символ не ломает определение`() {
        val full = "Русский текст".toByteArray(Charsets.UTF_8)
        // Отрезаем последний байт многобайтового символа.
        val truncated = full.copyOf(full.size - 1)
        assertTrue(
            "иначе любой русский текст в UTF-8 через раз определялся бы как 1251",
            NovelLibrary.isValidUtf8(truncated),
        )
    }
}
