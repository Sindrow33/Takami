package dev.anime.player.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimeCatalogTest {

    @Test
    fun readsEpisodeNumberFromCommonFileNames() {
        assertEquals(1, AnimeCatalog.episodeNumber("01 - Начало.mp4"))
        assertEquals(12, AnimeCatalog.episodeNumber("Серия 12.mkv"))
        assertEquals(7, AnimeCatalog.episodeNumber("ep07.webm"))
        assertEquals(3, AnimeCatalog.episodeNumber("3.mp4"))
        assertNull(AnimeCatalog.episodeNumber("intro.mp4"))
    }

    @Test
    fun ignoresNumbersInExtensionOnly() {
        assertNull(AnimeCatalog.episodeNumber("openning.mp4"))
    }

    @Test
    fun detectsVideoFilesCaseInsensitively() {
        assertTrue(AnimeCatalog.isVideo("a.MKV"))
        assertTrue(AnimeCatalog.isVideo("stream.m3u8"))
        assertFalse(AnimeCatalog.isVideo("cover.jpg"))
        assertFalse(AnimeCatalog.isVideo("noext"))
    }

    @Test
    fun sortsByEpisodeNumberAndPushesUnnumberedToTheEnd() {
        val eps = listOf(
            episode("intro", 0),
            episode("10", 10),
            episode("2", 2),
            episode("1", 1),
        )
        assertEquals(
            listOf("1", "2", "10", "intro"),
            AnimeCatalog.sortEpisodes(eps).map { it.title },
        )
    }

    private fun episode(title: String, number: Int) = AnimeCatalog.Episode(
        id = title, title = title, number = number, url = "file:///$title", isLocal = true,
    )
}

class AnimeCatalogTreeTest {

    @org.junit.Test
    fun scansAnimeSubfolderOfTheChosenDirectory() {
        val base = java.io.File.createTempFile("root", "").let { f ->
            f.delete(); f.mkdirs(); f
        }
        val chapterDir = java.io.File(base, "anime/My Title").apply { mkdirs() }
        java.io.File(chapterDir, "02 - second.mp4").writeText("x")
        java.io.File(chapterDir, "01 - first.mkv").writeText("x")
        java.io.File(chapterDir, "cover.jpg").writeText("x")

        val titles = AnimeCatalog.scan(base)

        org.junit.Assert.assertEquals(1, titles.size)
        org.junit.Assert.assertEquals("My Title", titles[0].name)
        org.junit.Assert.assertEquals(
            listOf(1, 2),
            titles[0].episodes.map { it.number },
        )
        base.deleteRecursively()
    }

    @org.junit.Test
    fun skipsFoldersWithoutVideoFiles() {
        val base = java.io.File.createTempFile("root2", "").let { f -> f.delete(); f.mkdirs(); f }
        java.io.File(base, "anime/images only").apply { mkdirs() }
            .let { java.io.File(it, "1.jpg").writeText("x") }

        org.junit.Assert.assertTrue(AnimeCatalog.scan(base).isEmpty())
        base.deleteRecursively()
    }
}
