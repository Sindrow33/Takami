package dev.takami.app

import dev.takami.app.library.DocumentPaths
import dev.takami.app.library.LibraryNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Разбор имён и путей — чистые функции над строками. Написаны так
 * намеренно: `Uri` и `DocumentFile` в JVM-тестах заглушки, падающие с
 * «Stub!», и логика поверх них была бы непроверяема.
 */
class LibraryNamesTest {

    @Test
    fun `страницы без ведущих нулей идут по числу, а не по строке`() {
        val given = listOf("10.jpg", "2.jpg", "1.jpg", "21.jpg", "3.jpg")
        assertEquals(
            "сортировка строкой ставит 10 перед 2 — глава читалась бы вперемешку",
            listOf("1.jpg", "2.jpg", "3.jpg", "10.jpg", "21.jpg"),
            LibraryNames.pageOrder(given),
        )
    }

    @Test
    fun `имена без числа не теряются и уходят в конец`() {
        val given = listOf("cover.jpg", "1.jpg", "2.jpg")
        val ordered = LibraryNames.pageOrder(given)
        assertEquals(listOf("1.jpg", "2.jpg", "cover.jpg"), ordered)
        assertEquals("ни одна страница не должна пропасть при сортировке", given.size, ordered.size)
    }

    @Test
    fun `архивы и картинки различаются по расширению в любом регистре`() {
        assertTrue(LibraryNames.isArchive("Глава 1.CBZ"))
        assertTrue(LibraryNames.isArchive("ch1.zip"))
        assertFalse(LibraryNames.isArchive("ch1"))
        assertTrue(LibraryNames.isImage("0001.WebP"))
        assertFalse(LibraryNames.isImage("thumbs.db"))
    }

    @Test
    fun `номер главы читается и с запятой`() {
        assertEquals(10.5f, LibraryNames.numberOf("Глава 10,5")!!, 0.001f)
        assertEquals(7f, LibraryNames.numberOf("Chapter 7")!!, 0.001f)
    }

    @Test
    fun `путь внутренней памяти показывается человеку понятно`() {
        assertEquals(
            "Внутренняя память/Manga",
            DocumentPaths.readable("content://com.android.externalstorage.documents/tree/primary%3AManga"),
        )
    }

    @Test
    fun `идентификатор карты не показывается как есть`() {
        assertEquals(
            "пользователь знает её как SD-карту, а не как 1A2B-3C4D",
            "SD-карта/Comics",
            DocumentPaths.readable("content://com.android.externalstorage.documents/tree/1A2B-3C4D%3AComics"),
        )
    }

    @Test
    fun `незнакомый провайдер не ломает экран`() {
        val raw = "content://com.example.cloud/documents/xyz"
        assertEquals("лучше показать как есть, чем пустую строку", raw, DocumentPaths.readable(raw))
    }

    @Test
    fun `страницы архива и папки сортируются одинаково`() {
        // Внутри CBZ имена обычно с путём: "ch1/10.jpg". Разбор
        // расширения обязан это переживать, иначе страницы архива
        // отсеются как «не картинки» и глава откроется пустой.
        assertTrue(LibraryNames.isImage("Chapter 1/010.jpg"))
        assertEquals(
            listOf("1.png", "2.png", "10.png"),
            LibraryNames.pageOrder(listOf("10.png", "1.png", "2.png")),
        )
    }

    @Test
    fun `номер страницы берётся последним числом, а не номером главы`() {
        // Путь внутри архива схлопывается в имя, и первое число в нём —
        // номер главы, одинаковый у всех страниц: сортировка по нему
        // оставила бы порядок случайным.
        assertEquals(
            listOf("Chapter 1_001.jpg", "Chapter 1_002.jpg", "Chapter 1_010.jpg"),
            LibraryNames.pageOrder(
                listOf("Chapter 1_010.jpg", "Chapter 1_002.jpg", "Chapter 1_001.jpg"),
            ),
        )
        assertEquals(5f, LibraryNames.pageNumberOf("vol2_005.png")!!, 0.001f)
    }

    @Test
    fun `служебные записи macOS не считаются страницами`() {
        assertFalse(
            "файлы-двойники дают вдвое больше страниц, половина из которых не декодируется",
            LibraryNames.isPageEntry("__MACOSX/ch1/._0001.jpg"),
        )
        assertFalse(LibraryNames.isPageEntry("ch1/._0001.jpg"))
        assertFalse(LibraryNames.isPageEntry("ch1/.hidden.jpg"))
        assertTrue(LibraryNames.isPageEntry("ch1/0001.jpg"))
        assertFalse(LibraryNames.isPageEntry("ComicInfo.xml"))
    }

    @Test
    fun `имя записи архива не выходит за пределы каталога`() {
        assertEquals("etc_passwd.jpg", LibraryNames.safeEntryName("../../etc/passwd.jpg"))
        assertEquals("ch1_0001.jpg", LibraryNames.safeEntryName("ch1/0001.jpg"))
        assertEquals(
            "архивы из Windows несут обратные слэши",
            "ch1_0001.jpg",
            LibraryNames.safeEntryName("ch1\\0001.jpg"),
        )
    }
}
