package com.mangareader.reader.engine

import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.cache.DiskLruPageCache
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Сетевой источник сохраняет страницы на диск сам — ему нужен путь, а
 * не байты в памяти. Значит кеш обязан не копировать эти файлы (иначе
 * каждая страница лежит дважды), но обязан считать и вытеснять их:
 * у источника нет ни лимита, ни LRU, и на длинной манге его каталог
 * растёт до десятков гигабайт.
 */
class AdoptedDirCacheTest {

    private fun tempDir(prefix: String): File = Files.createTempDirectory(prefix).toFile()

    private fun jpeg(size: Int) = ByteArray(size) { 0x20 }.also {
        it[0] = 0xFF.toByte(); it[1] = 0xD8.toByte(); it[2] = 0xFF.toByte()
    }

    private fun cacheWith(own: File, adopted: File, maxBytes: Long): DiskLruPageCache =
        DiskLruPageCache(own, maxBytes).apply { adopt(adopted) }

    @Test
    fun `файлы источника учитываются в размере кеша`() {
        val own = tempDir("own")
        val downloads = tempDir("web-pages")
        val cache = cacheWith(own, downloads, maxBytes = 10_000)

        File(downloads, "page1").writeBytes(jpeg(500))
        File(downloads, "page2").writeBytes(jpeg(700))

        assertEquals(
            1200L, cache.sizeBytes(),
            "каталог источника не учитывался бы, и пользователь видел бы нулевой размер при забитом диске",
        )
    }

    @Test
    fun `файл источника опознаётся как уже управляемый`() {
        val own = tempDir("own")
        val downloads = tempDir("web-pages")
        val cache = cacheWith(own, downloads, maxBytes = 10_000)

        val downloaded = File(downloads, "page1").apply { writeBytes(jpeg(128)) }
        val foreign = File(tempDir("elsewhere"), "page1").apply { writeBytes(jpeg(128)) }

        assertTrue(cache.isManaged(downloaded), "копировать такой файл значит хранить страницу дважды")
        assertFalse(cache.isManaged(foreign))
    }

    @Test
    fun `лимит вытесняет файлы источника, а не только свои`() = runTest {
        val own = tempDir("own")
        val downloads = tempDir("web-pages")
        val cache = cacheWith(own, downloads, maxBytes = 1_000)

        repeat(20) { i ->
            File(downloads, "page$i").apply {
                writeBytes(jpeg(200))
                setLastModified(1_000L + i)
            }
        }
        cache.enforceLimit()

        assertTrue(
            cache.sizeBytes() <= 1_000,
            "каталог источника перерос лимит: ${cache.sizeBytes()} — у него самого вытеснения нет",
        )
    }

    @Test
    fun `вытесняются самые старые, свежие остаются`() = runTest {
        val own = tempDir("own")
        val downloads = tempDir("web-pages")
        val cache = cacheWith(own, downloads, maxBytes = 600)

        val oldest = File(downloads, "old").apply { writeBytes(jpeg(300)); setLastModified(1_000L) }
        val newest = File(downloads, "new").apply { writeBytes(jpeg(300)); setLastModified(9_000L) }
        File(downloads, "middle").apply { writeBytes(jpeg(300)); setLastModified(5_000L) }

        cache.enforceLimit()

        assertTrue(newest.exists(), "только что прочитанная страница не должна вытесняться первой")
        assertFalse(oldest.exists(), "самая давняя страница обязана уйти первой")
    }

    @Test
    fun `очистка убирает и свои файлы, и файлы источника`() = runTest {
        val own = tempDir("own")
        val downloads = tempDir("web-pages")
        val cache = cacheWith(own, downloads, maxBytes = 10_000)

        cache.put(PageRef(id = "p", index = 0, uri = "https://cdn/p.jpg"), jpeg(128))
        File(downloads, "page1").writeBytes(jpeg(128))

        cache.clear()
        assertEquals(0L, cache.sizeBytes(), "кнопка очистки обязана освобождать всё занятое место, а не половину")
    }

    @Test
    fun `повторное взятие каталога не удваивает подсчёт`() {
        val own = tempDir("own")
        val downloads = tempDir("web-pages")
        val cache = cacheWith(own, downloads, maxBytes = 10_000)
        cache.adopt(downloads)
        cache.adopt(downloads)

        File(downloads, "page1").writeBytes(jpeg(500))
        assertEquals(500L, cache.sizeBytes())
    }
}
