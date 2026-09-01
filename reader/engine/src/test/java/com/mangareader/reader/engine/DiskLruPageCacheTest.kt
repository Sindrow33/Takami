package com.mangareader.reader.engine

import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.cache.DiskLruPageCache
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiskLruPageCacheTest {

    private fun cache(maxBytes: Long = 1024L * 1024) = DiskLruPageCache(
        directory = Files.createTempDirectory("pagecache").toFile(),
        maxSizeBytes = maxBytes,
    )

    private fun page(uri: String, headers: Map<String, String> = emptyMap()) =
        PageRef(id = uri, index = 0, uri = uri, headers = headers)

    @Test
    fun `один URL с разным Referer — разные записи`() {
        val c = cache()
        val a = page("https://cdn/p1.jpg", mapOf("Referer" to "https://siteA/"))
        val b = page("https://cdn/p1.jpg", mapOf("Referer" to "https://siteB/"))

        assertNotEquals(
            c.keyFor(a.uri, a.headers),
            c.keyFor(b.uri, b.headers),
            "один URL с разным Referer у хостингов картинок отдаёт разное — склеивать нельзя",
        )
    }

    @Test
    fun `заглушка с 403 не подменяет настоящую страницу`() = runTest {
        val c = cache()
        val withReferer = page("https://cdn/p1.jpg", mapOf("Referer" to "https://site/"))
        val withoutReferer = page("https://cdn/p1.jpg")

        c.put(withoutReferer, "403 forbidden".toByteArray())

        // Запрос с правильным Referer не должен получить сохранённую заглушку.
        assertNull(
            c.get(withReferer),
            "страница с Referer обязана качаться заново, а не брать заглушку из кеша",
        )
    }

    @Test
    fun `порядок и регистр заголовков не меняют ключ`() {
        val c = cache()
        val k1 = c.keyFor("https://cdn/p.jpg", linkedMapOf("Referer" to "r", "Origin" to "o"))
        val k2 = c.keyFor("https://cdn/p.jpg", linkedMapOf("origin" to "o", "REFERER" to "r"))
        assertEquals(k1, k2, "один и тот же запрос не должен давать две записи из-за регистра")
    }

    @Test
    fun `не влияющие на ответ заголовки не ломают попадание в кеш`() {
        val c = cache()
        val k1 = c.keyFor("https://cdn/p.jpg", mapOf("User-Agent" to "v1", "Referer" to "r"))
        val k2 = c.keyFor("https://cdn/p.jpg", mapOf("User-Agent" to "v2", "Referer" to "r"))
        assertEquals(
            k1, k2,
            "меняющийся User-Agent промахивался бы мимо кеша на каждом запросе",
        )
    }

    @Test
    fun `сохранённая страница читается обратно`() = runTest {
        val c = cache()
        val p = page("https://cdn/p1.jpg", mapOf("Referer" to "https://site/"))
        c.put(p, ByteArray(64) { 7 })

        val got = c.get(p)
        assertNotNull(got)
        assertEquals(64, got.length().toInt())
    }

    @Test
    fun `пустой файл не считается попаданием`() = runTest {
        val c = cache()
        val p = page("https://cdn/p1.jpg")
        c.fileFor(p).writeBytes(ByteArray(0))
        assertNull(c.get(p), "нулевой файл — это оборванная загрузка, а не кеш")
    }

    @Test
    fun `превышение лимита вытесняет старые записи`() = runTest {
        val c = cache(maxBytes = 300)
        repeat(10) { i ->
            c.put(page("https://cdn/p$i.jpg"), ByteArray(100) { 1 })
        }
        assertTrue(c.sizeBytes() <= 300, "кеш вырос выше лимита: ${c.sizeBytes()}")
    }

    @Test
    fun `очистка опустошает кеш`() = runTest {
        val c = cache()
        c.put(page("https://cdn/p1.jpg"), ByteArray(128))
        assertTrue(c.sizeBytes() > 0)
        c.clear()
        assertEquals(0L, c.sizeBytes())
    }
}
