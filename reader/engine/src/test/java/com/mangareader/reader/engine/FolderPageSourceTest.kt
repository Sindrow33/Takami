package com.mangareader.reader.engine

import com.mangareader.core.model.PageLoad
import com.mangareader.reader.engine.source.FolderPageSource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FolderPageSourceTest {

    private fun tempLibrary(): File {
        val root = Files.createTempDirectory("manga-lib").toFile()
        listOf("ch01", "ch02").forEach { chapter ->
            val dir = File(root, chapter).apply { mkdirs() }
            listOf("0001.jpg", "0002.png", "0003.webp").forEach { name ->
                File(dir, name).writeBytes(ByteArray(16) { 1 })
            }
            File(dir, "ComicInfo.xml").writeText("<xml/>")
        }
        return root
    }

    @Test
    fun `страницы отдаются в порядке чтения и без нехватких файлов`() = runTest {
        val root = tempLibrary()
        val source = FolderPageSource(root, listOf("ch01", "ch02"))

        val pages = source.pages("ch01")
        assertEquals(3, pages.size, "нечитаемые файлы вроде ComicInfo.xml не должны попадать в страницы")
        assertEquals(listOf(0, 1, 2), pages.map { it.index })
        assertTrue(pages.first().uri.endsWith("0001.jpg"))
    }

    @Test
    fun `навигация по главам упирается в края серии`() = runTest {
        val source = FolderPageSource(tempLibrary(), listOf("ch01", "ch02"))

        assertEquals("ch02", source.nextChapter("ch01"))
        assertNull(source.nextChapter("ch02"))
        assertEquals("ch01", source.prevChapter("ch02"))
        assertNull(source.prevChapter("ch01"))
    }

    @Test
    fun `open отдаёт ровно один терминальный элемент`() = runTest {
        val root = tempLibrary()
        val source = FolderPageSource(root, listOf("ch01"))
        val page = source.pages("ch01").first()

        val loads = source.open(page).toList()
        val terminal = loads.filter { it is PageLoad.Done || it is PageLoad.Error }
        assertEquals(1, terminal.size)
        assertTrue(terminal.single() is PageLoad.Done)
        assertTrue((terminal.single() as PageLoad.Done).file.exists())
    }

    @Test
    fun `отсутствующий файл превращается в ошибку, а не в исключение`() = runTest {
        val root = tempLibrary()
        val source = FolderPageSource(root, listOf("ch01"))
        val page = source.pages("ch01").first()
        File(java.net.URI(page.uri)).delete()

        val loads = source.open(page).toList()
        assertTrue(loads.single() is PageLoad.Error)
    }

    @Test
    fun `неизвестная глава даёт пустой список, а не падение`() = runTest {
        val source = FolderPageSource(tempLibrary(), listOf("ch01"))
        assertTrue(source.pages("ch99").isEmpty())
    }
}
