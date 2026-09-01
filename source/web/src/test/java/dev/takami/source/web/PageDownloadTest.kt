package dev.takami.source.web

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Проверяет то, из-за чего этот путь и переписывался: байты картинки
 * не должны проходить через String, а оборванная закачка не должна
 * оставлять на диске файл, который читалка примет за готовый.
 */
class PageDownloadTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Байты, которые невалидны в UTF-8 — как настоящий JPEG. */
    private val jpegLike = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
        0x00, 0x10, 0x4A, 0x46, 0x49, 0x46,
        0x80.toByte(), 0x81.toByte(), 0xFE.toByte(), 0xC3.toByte(),
        0xFF.toByte(), 0xD9.toByte(),
    )

    /** Загрузчик, который пишет заданные байты потоком, как боевой. */
    private class FakeFetcher(
        private val bytes: ByteArray,
        private val failAfter: Int? = null,
    ) : PageFetcher {
        var seenHeaders: Map<String, String> = emptyMap()

        override suspend fun html(url: String, headers: Map<String, String>): String = ""

        override suspend fun download(
            url: String,
            headers: Map<String, String>,
            target: File,
            onProgress: (Long, Long?) -> Unit,
        ) {
            seenHeaders = headers
            val part = File(target.parentFile, "incomplete/" + target.name + ".part")
            part.parentFile?.mkdirs()
            part.outputStream().use { out ->
                val limit = failAfter ?: bytes.size
                out.write(bytes, 0, limit)
                out.flush()
                if (failAfter != null) {
                    part.delete()
                    throw PageSourceException("обрыв на $failAfter байт")
                }
            }
            onProgress(bytes.size.toLong(), bytes.size.toLong())
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
        }
    }

    @Test
    fun `бинарные байты доходят до файла без порчи`() = runTest {
        val target = File(tmp.newFolder(), "page")
        FakeFetcher(jpegLike).download("https://cdn/a.jpg", emptyMap(), target)

        assertArrayEquals(jpegLike, target.readBytes())
    }

    @Test
    fun `круговой путь через String портит байты — почему нельзя брать get`() {
        // Именно это и делал прежний код: HttpResponse.body -> String.
        val viaString = String(jpegLike, Charsets.UTF_8).toByteArray(Charsets.UTF_8)

        assertFalse(
            "если это когда-нибудь станет равным, проверку можно снять",
            jpegLike.contentEquals(viaString),
        )
    }

    @Test
    fun `оборванная закачка не оставляет файла`() = runTest {
        val dir = tmp.newFolder()
        val target = File(dir, "page")

        runCatching {
            FakeFetcher(jpegLike, failAfter = 4).download("https://cdn/a.jpg", emptyMap(), target)
        }

        assertFalse("огрызок не должен доехать до целевого файла", target.exists())
        assertTrue(
            "временный .part тоже не должен остаться",
            dir.walkTopDown().none { it.name.endsWith(".part") },
        )
    }

    @Test
    fun `незавершённая закачка лежит вне каталога готовых страниц`() = runTest {
        val dir = tmp.newFolder()
        val target = File(dir, "page")

        FakeFetcher(jpegLike).download("https://cdn/a.jpg", emptyMap(), target)

        // Верхний уровень каталога считает и вытесняет дисковый кеш
        // читалки — временным файлам там быть нельзя.
        assertEquals(
            listOf("incomplete", "page"),
            dir.listFiles()!!.map { it.name }.sorted(),
        )
    }

    @Test
    fun `заголовки страницы доходят до загрузчика`() = runTest {
        val fetcher = FakeFetcher(jpegLike)
        val headers = mapOf("Referer" to "https://example.org/ch-1", "Origin" to "https://example.org")

        fetcher.download("https://cdn/a.jpg", headers, File(tmp.newFolder(), "page"))

        assertEquals("https://example.org/ch-1", fetcher.seenHeaders["Referer"])
        assertEquals("https://example.org", fetcher.seenHeaders["Origin"])
    }
}
