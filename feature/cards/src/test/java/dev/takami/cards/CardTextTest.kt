package dev.takami.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTextTest {

    @Test
    fun kindLabelsAreLocalised() {
        assertEquals("Аниме", CardText.kindLabel(ContentKind.Anime))
        assertEquals("Манга", CardText.kindLabel(ContentKind.Manga))
        assertEquals("Ранобэ", CardText.kindLabel(ContentKind.Novel))
    }

    @Test
    fun unknownRoleHasNoLabel() {
        assertEquals("Главный", CardText.roleLabel(CharacterRole.Main))
        assertNull(CardText.roleLabel(CharacterRole.Unknown))
    }

    @Test
    fun badgeCollapsesLargeCounts() {
        assertNull(CardText.badgeLabel(0))
        assertNull(CardText.badgeLabel(-3))
        assertEquals("7", CardText.badgeLabel(7))
        assertEquals("99", CardText.badgeLabel(99))
        assertEquals("99+", CardText.badgeLabel(100))
        assertEquals("99+", CardText.badgeLabel(5000))
    }

    @Test
    fun initialsHandleOneAndManyWords() {
        assertEquals("СА", CardText.initials("Стальной алхимик"))
        assertEquals("БЕ", CardText.initials("Берсерк"))
        assertEquals("?", CardText.initials("   "))
        assertEquals("ОП", CardText.initials("Опенинг  Первый  Лишний"))
    }

    @Test
    fun initialsSurviveExtraWhitespace() {
        assertEquals("АБ", CardText.initials("  Альфа   Бета  "))
    }

    @Test
    fun placeholderColourIsStableForTheSameId() {
        val first = CardText.placeholderIndex("web:abc", 6)
        val second = CardText.placeholderIndex("web:abc", 6)
        assertEquals(first, second)
    }

    @Test
    fun placeholderIndexIsAlwaysInRange() {
        listOf("a", "zzzzzzzzzzzz", "web:очень длинный ключ", "").forEach { id ->
            val index = CardText.placeholderIndex(id, 6)
            assertTrue("index=$index for id=$id", index in 0..5)
        }
    }

    @Test
    fun emptyPaletteDoesNotCrash() {
        assertEquals(0, CardText.placeholderIndex("abc", 0))
    }

    @Test
    fun progressIsHiddenWhenZeroAndClamped() {
        assertFalse(CardText.showProgress(0f))
        assertTrue(CardText.showProgress(0.4f))
        assertEquals(1f, CardText.clampProgress(2f), 0.001f)
        assertEquals(0f, CardText.clampProgress(-1f), 0.001f)
    }
    @Test
    fun glyphIsOneUppercaseCharacter() {
        // Макет берёт ровно первую букву (`c.n.charAt(0)`), а не инициалы:
        // при кегле 58sp вторая буква не влезает в карточку 96dp.
        assertEquals("К", CardText.glyph("Кэнджи Ямада"))
        assertEquals("A", CardText.glyph("akira"))
        assertEquals("Я", CardText.glyph("  Яэ  "))
        assertEquals("?", CardText.glyph("   "))
        assertEquals("?", CardText.glyph(""))
    }

    @Test
    fun kindShortIsSingleLetterPerKind() {
        val shorts = ContentKind.values().map { CardText.kindShort(it) }
        // Разные буквы: чип 24dp несёт только их, и совпадение сделало бы
        // тип неразличимым.
        assertEquals(shorts.size, shorts.toSet().size)
        shorts.forEach { assertEquals(1, it.length) }
    }
}
