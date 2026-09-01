package dev.takami.app

import dev.takami.app.home.ContentType
import dev.takami.app.home.LibraryFeed
import dev.takami.app.home.prefix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFeedTest {

    private fun card(
        id: String,
        kind: ContentType = ContentType.Manga,
        progress: Float = 0f,
    ) = LibraryFeed.TitleCardData(id = id, title = id, kind = kind, progress = progress)

    @Test
    fun `пустая лента распознаётся как пустая`() {
        val feed = LibraryFeed.Feed(
            continueReading = emptyList(),
            manga = emptyList(),
            novels = emptyList(),
            anime = emptyList(),
            folderChosen = false,
            hasSources = false,
        )
        assertTrue(feed.isEmpty)
        assertNull("hero из пустой ленты — это как раз выдуманная карточка", feed.hero)
    }

    @Test
    fun `лента с одними ранобэ не считается пустой`() {
        val feed = LibraryFeed.Feed(
            continueReading = emptyList(),
            manga = emptyList(),
            novels = listOf(card("novel:книга", ContentType.Novel)),
            anime = emptyList(),
            folderChosen = true,
            hasSources = false,
        )
        assertTrue("раздел ранобэ — такое же наполнение, как манга", !feed.isEmpty)
    }

    @Test
    fun `hero берётся из продолжения чтения`() {
        val started = card("manga:начатая", progress = .4f)
        val feed = LibraryFeed.Feed(
            continueReading = listOf(started),
            manga = listOf(started),
            novels = emptyList(),
            anime = emptyList(),
            folderChosen = true,
            hasSources = false,
        )
        assertEquals(started, feed.hero)
    }

    @Test
    fun `идентификатор несёт префикс источника`() {
        // Иначе локальный и сетевой тайтл с одинаковым номером
        // схлопнутся в одну карточку.
        assertEquals("manga", ContentType.Manga.prefix)
        assertEquals("novel", ContentType.Novel.prefix)
        assertEquals("anime", ContentType.Anime.prefix)
    }

    @Test
    fun `русские числительные согласуются`() {
        // «1 глав» и «5 глава» заметны сильнее, чем кажется.
        assertEquals("глава", LibraryFeed.plural(1, "глава", "главы", "глав"))
        assertEquals("главы", LibraryFeed.plural(3, "глава", "главы", "глав"))
        assertEquals("глав", LibraryFeed.plural(5, "глава", "главы", "глав"))
        assertEquals("глав", LibraryFeed.plural(11, "глава", "главы", "глав"))
        assertEquals("глава", LibraryFeed.plural(21, "глава", "главы", "глав"))
        assertEquals("глав", LibraryFeed.plural(0, "глава", "главы", "глав"))
    }

    @Test
    fun `у карточки без обложки нет заглушечной ссылки`() {
        assertNull(
            "фиктивный URL дал бы сломанную картинку вместо честного плейсхолдера",
            card("manga:x").coverUrl,
        )
    }
}
