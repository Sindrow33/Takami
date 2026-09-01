package com.mangareader.reader.engine

import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.cache.DiskLruPageCache
import com.mangareader.reader.engine.cache.ImageBytes
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageBytesTest {

    private fun withSignature(vararg prefix: Int): ByteArray =
        ByteArray(256).also { body ->
            prefix.forEachIndexed { i, v -> body[i] = v.toByte() }
        }

    private fun jpeg() = withSignature(0xFF, 0xD8, 0xFF, 0xE0)
    private fun png() = withSignature(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Test
    fun `реальные форматы распознаются`() {
        assertTrue(ImageBytes.looksLikeImage(jpeg()))
        assertTrue(ImageBytes.looksLikeImage(png()))

        val webp = ByteArray(256)
        "RIFF".forEachIndexed { i, c -> webp[i] = c.code.toByte() }
        "WEBP".forEachIndexed { i, c -> webp[8 + i] = c.code.toByte() }
        assertTrue(ImageBytes.looksLikeImage(webp), "WebP приходит по адресам с .jpg — обязан распознаваться")
    }

    @Test
    fun `картинка, прошедшая через String, не считается картинкой`() {
        // Ровно тот дефект: байты декодированы как UTF-8 и закодированы
        // обратно — всё, что вне ASCII, заменено на U+FFFD.
        val original = jpeg()
        val corrupted = String(original, Charsets.UTF_8).toByteArray(Charsets.UTF_8)

        assertTrue(ImageBytes.looksLikeImage(original))
        assertFalse(
            ImageBytes.looksLikeImage(corrupted),
            "тело картинки, прогнанное через String, обязано отсеиваться до попадания в кеш",
        )
    }

    @Test
    fun `HTML с капчей под кодом 200 не считается картинкой`() {
        val html = "<!DOCTYPE html><html><body>Проверка, что вы не робот…</body></html>"
            .repeat(4).toByteArray()
        assertFalse(ImageBytes.looksLikeImage(html))
    }

    @Test
    fun `оборванная загрузка не считается картинкой`() {
        assertFalse(ImageBytes.looksLikeImage(ByteArray(0)))
        assertFalse(ImageBytes.looksLikeImage(byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
    }
}

class DiskCacheValidationTest {

    private fun cache() = DiskLruPageCache(
        directory = Files.createTempDirectory("cache-validation").toFile(),
    )

    private fun page(uri: String) = PageRef(id = uri, index = 0, uri = uri)

    private fun jpeg() = ByteArray(256).also {
        it[0] = 0xFF.toByte(); it[1] = 0xD8.toByte(); it[2] = 0xFF.toByte()
    }

    @Test
    fun `битые байты не попадают в кеш`() = runTest {
        val c = cache()
        val p = page("https://cdn/p1.jpg")

        val saved = c.put(p, "<html>капча</html>".repeat(10).toByteArray())
        assertNull(saved, "мусор в кеше живёт неделями и отдаётся при каждом открытии главы")
        assertNull(c.get(p))
    }

    @Test
    fun `нормальная картинка сохраняется как раньше`() = runTest {
        val c = cache()
        val p = page("https://cdn/p1.jpg")

        assertNotNull(c.put(p, jpeg()))
        assertNotNull(c.get(p))
    }

    @Test
    fun `запись, испорченная в обход put, отбраковывается при чтении и удаляется`() = runTest {
        val c = cache()
        val p = page("https://cdn/p1.jpg")
        // Имитируем запись, легшую до появления проверки.
        c.fileFor(p).writeBytes(ByteArray(256) { 0x41 })

        assertNull(c.get(p), "старая битая запись обязана отбраковываться при чтении")
        assertFalse(c.fileFor(p).exists(), "и удаляться, иначе она занимает место вечно")
    }
}
