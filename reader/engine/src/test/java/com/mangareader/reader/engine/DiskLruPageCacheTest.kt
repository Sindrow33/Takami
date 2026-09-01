package com.mangareader.reader.engine

import com.mangareader.core.model.PageCacheKey
import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.cache.DiskLruPageCache
import kotlinx.coroutines.test.runTest
import java.io.File
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

    /**
     * Валидные байты JPEG нужного размера. Кеш отбраковывает всё, что
     * не похоже на изображение, поэтому тестовые данные обязаны нести
     * настоящую сигнатуру — иначе тест проверяет отбраковку, а не то,
     * что заявлено в его названии.
     */
    private fun jpegBytes(size: Int = 128) = ByteArray(size) { 0x20 }.also {
        it[0] = 0xFF.toByte()
        it[1] = 0xD8.toByte()
        it[2] = 0xFF.toByte()
    }

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

        c.put(withoutReferer, jpegBytes())

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
        c.put(p, jpegBytes(size = 128))

        val got = c.get(p)
        assertNotNull(got)
        assertEquals(128, got.length().toInt())
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
            c.put(page("https://cdn/p$i.jpg"), jpegBytes(size = 100))
        }
        assertTrue(c.sizeBytes() <= 300, "кеш вырос выше лимита: ${c.sizeBytes()}")
    }

    @Test
    fun `очистка опустошает кеш`() = runTest {
        val c = cache()
        c.put(page("https://cdn/p1.jpg"), jpegBytes(size = 128))
        assertTrue(c.sizeBytes() > 0)
        c.clear()
        assertEquals(0L, c.sizeBytes())
    }

    @Test
    fun `файл источника в поднадзорном каталоге — попадание в кеш`() = runTest {
        val dir = Files.createTempDirectory("pagecache").toFile()
        val adopted = Files.createTempDirectory("web-pages").toFile()
        val c = DiskLruPageCache(directory = dir, maxSizeBytes = 1024L * 1024)
        c.adopt(adopted)

        val p = page("https://cdn/p1.jpg", mapOf("Referer" to "https://site/"))
        // Так делает сетевой источник: пишет страницу сам, в свой
        // каталог, под именем из общей формулы ключа.
        File(adopted, PageCacheKey.of(p)).writeBytes(jpegBytes(size = 128))

        assertNotNull(
            c.get(p),
            "кеш отвечает за место этих файлов — значит обязан их и находить",
        )
    }

    @Test
    fun `имя файла у источника и ключ кеша совпадают`() {
        val c = cache()
        val p = page("https://cdn/p1.jpg", mapOf("Referer" to "https://site/", "User-Agent" to "x"))
        assertEquals(
            PageCacheKey.of(p), c.keyFor(p.uri, p.headers),
            "две стороны считают имя одного файла — разойтись им нельзя",
        )
    }

    @Test
    fun `пустые заголовки дают тот же ключ, что и общая формула`() {
        val c = cache()
        assertEquals(
            PageCacheKey.of("https://cdn/p.jpg"),
            c.keyFor("https://cdn/p.jpg"),
            "именно на пустом случае две прежние реализации и разъезжались",
        )
    }

    @Test
    fun `брошенный огрызок убирается, свежий остаётся`() = runTest {
        val dir = Files.createTempDirectory("pagecache").toFile()
        val c = DiskLruPageCache(directory = dir, maxSizeBytes = 1024L * 1024)
        val incomplete = File(dir, DiskLruPageCache.INCOMPLETE_DIR).apply { mkdirs() }

        val stale = File(incomplete, "old.part").apply { writeBytes(ByteArray(500)) }
        stale.setLastModified(System.currentTimeMillis() - DiskLruPageCache.STALE_INCOMPLETE_AGE_MS - 1000)
        val fresh = File(incomplete, "live.part").apply { writeBytes(ByteArray(500)) }

        c.enforceLimit()

        assertTrue(!stale.exists(), "огрызок, в который час никто не писал, — мусор")
        assertTrue(fresh.exists(), "удалить файл идущей закачки значит уронить страницу")
    }

    @Test
    fun `огрызки входят в занятое место и в очистку`() = runTest {
        val dir = Files.createTempDirectory("pagecache").toFile()
        val c = DiskLruPageCache(directory = dir, maxSizeBytes = 1024L * 1024)
        File(dir, DiskLruPageCache.INCOMPLETE_DIR).apply { mkdirs() }
            .let { File(it, "x.part").writeBytes(ByteArray(700)) }

        assertEquals(700L, c.sizeBytes(), "место занято настоящее — его надо показывать")
        c.clear()
        assertEquals(0L, c.sizeBytes(), "очистка обязана освобождать всё, за что кеш отвечает")
    }
}
