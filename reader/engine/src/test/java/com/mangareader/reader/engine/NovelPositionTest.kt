package com.mangareader.reader.engine

import com.mangareader.reader.engine.novel.NovelChapter
import com.mangareader.reader.engine.novel.NovelPaginator
import com.mangareader.reader.engine.novel.NovelPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NovelPositionTest {

    private fun chapter(vararg paragraphs: String) =
        NovelChapter(id = "ch1", title = null, paragraphs = paragraphs.toList())

    @Test
    fun `позиция не уезжает при смене размера шрифта`() {
        val ch = chapter("a".repeat(500), "b".repeat(500))
        val offset = 600

        val small = NovelPaginator.paginate(ch, charsPerScreen = 100)
        val large = NovelPaginator.paginate(ch, charsPerScreen = 400)

        // Разметка разная, но символ остаётся тем же символом: экран
        // меняется, а прочитанная доля — нет. Ради этого позиция и
        // хранится в символах, а не в пикселях прокрутки.
        assertEquals(
            NovelPosition.progress(offset, ch.totalChars),
            NovelPosition.progress(offset, ch.totalChars),
        )
        assertTrue(NovelPaginator.screenOf(small, offset) > NovelPaginator.screenOf(large, offset))
    }

    @Test
    fun `смещения абзацев считаются с разделителем`() {
        val ch = chapter("абв", "где")
        assertEquals(listOf(0, 4), ch.paragraphOffsets)
        assertEquals(
            7, ch.totalChars,
            "длина, посчитанная без разделителей, разъедется с разметкой тем сильнее, чем больше абзацев",
        )
    }

    @Test
    fun `доля прочитанного не делится на ноль на пустой главе`() {
        val ch = chapter()
        assertEquals(0, ch.totalChars)
        assertEquals(0f, NovelPosition.progress(0, ch.totalChars))
    }

    @Test
    fun `доля прочитанного не выходит за границы`() {
        assertEquals(1f, NovelPosition.progress(999, 100))
        assertEquals(0f, NovelPosition.progress(-5, 100))
    }

    @Test
    fun `перевод доли в смещение и обратно устойчив`() {
        val total = 1000
        val offset = NovelPosition.offsetOf(0.42f, total)
        assertEquals(420, offset)
        assertTrue(kotlin.math.abs(NovelPosition.progress(offset, total) - 0.42f) < 0.01f)
    }

    @Test
    fun `абзац находится по смещению`() {
        val ch = chapter("абв", "где", "жзи")
        assertEquals(0, ch.paragraphAt(1))
        assertEquals(1, ch.paragraphAt(4))
        assertEquals(2, ch.paragraphAt(99), "смещение за концом не должно ронять читалку")
    }

    @Test
    fun `первый экран начинается с нуля всегда`() {
        assertEquals(listOf(0), NovelPaginator.paginate(chapter(), charsPerScreen = 100))
        assertEquals(
            0, NovelPaginator.paginate(chapter("текст"), charsPerScreen = 100).first(),
        )
    }

    @Test
    fun `экраны идут по возрастанию и покрывают главу`() {
        val ch = chapter("a".repeat(1000))
        val starts = NovelPaginator.paginate(ch, charsPerScreen = 120)
        assertEquals(starts.sorted(), starts, "границы экранов обязаны возрастать")
        assertTrue(starts.size > 1, "тысяча символов не помещается в один экран по 120")
        assertTrue(starts.last() < ch.totalChars, "пустой экран за концом главы не нужен")
    }

    @Test
    fun `нулевая вместимость экрана не зацикливает разметку`() {
        // До первого layout размеры неизвестны; бесконечный цикл здесь
        // означал бы зависание при открытии главы.
        assertEquals(listOf(0), NovelPaginator.paginate(chapter("текст"), charsPerScreen = 0))
    }

    @Test
    fun `номер экрана ищется по границам, а не делением`() {
        val starts = listOf(0, 100, 250, 400)
        assertEquals(0, NovelPaginator.screenOf(starts, 50))
        assertEquals(1, NovelPaginator.screenOf(starts, 100))
        assertEquals(2, NovelPaginator.screenOf(starts, 399))
        assertEquals(3, NovelPaginator.screenOf(starts, 100000))
    }

    @Test
    fun `вместимость экрана растёт при мелком шрифте`() {
        val small = NovelPaginator.estimateCharsPerScreen(1080, 2000, 32f, 1.6f, 40)
        val large = NovelPaginator.estimateCharsPerScreen(1080, 2000, 64f, 1.6f, 40)
        assertTrue(small > large, "мелкий шрифт обязан вмещать больше, иначе разметка перевёрнута")
    }

    @Test
    fun `нулевые размеры экрана дают нулевую вместимость, а не деление на ноль`() {
        assertEquals(0, NovelPaginator.estimateCharsPerScreen(0, 0, 18f, 1.6f, 20))
        assertEquals(0, NovelPaginator.estimateCharsPerScreen(1080, 2000, 0f, 1.6f, 20))
    }

    @Test
    fun `поля шире экрана не дают отрицательную вместимость`() {
        assertEquals(
            0, NovelPaginator.estimateCharsPerScreen(200, 2000, 18f, 1.6f, 200),
            "отрицательная ширина строки уронила бы разметку в бесконечный цикл",
        )
    }
}
