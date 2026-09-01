package dev.takami.source.web

import com.mangareader.core.model.PageRef
import core.model.ImagePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты читают поля на дальнем конце передачи, а не только факт записи:
 * три дефекта подряд в этом проекте были ровно «значение доезжает и
 * никто его не читает».
 */
class ChapterPlanTest {

    private val chapterUrl = "https://example.org/manga/title/ch-12?x=1"

    private fun img(index: Int, url: String, headers: Map<String, String> = emptyMap()) =
        ImagePage(index = index, url = url, headers = headers)

    @Test
    fun `referer подставляется каждой странице`() {
        val refs = ChapterPlan.toPageRefs(
            "ch12", chapterUrl,
            listOf(img(0, "https://cdn.example.org/1.jpg"), img(1, "https://cdn.example.org/2.jpg")),
        )

        assertEquals(2, refs.size)
        refs.forEach {
            assertEquals(chapterUrl, it.headers["Referer"])
            assertEquals("https://example.org", it.headers["Origin"])
        }
    }

    @Test
    fun `свой referer из парсера не перетирается`() {
        val refs = ChapterPlan.toPageRefs(
            "ch12", chapterUrl,
            listOf(img(0, "https://cdn.example.org/1.jpg", mapOf("Referer" to "https://other.test/"))),
        )

        assertEquals("https://other.test/", refs[0].headers["Referer"])
    }

    @Test
    fun `дырявая нумерация парсера превращается в плотную`() {
        val refs = ChapterPlan.toPageRefs(
            "ch12", chapterUrl,
            listOf(img(1, "https://cdn/a.jpg"), img(2, "https://cdn/b.jpg"), img(5, "https://cdn/c.jpg")),
        )

        assertEquals(listOf(0, 1, 2), refs.map(PageRef::index))
    }

    @Test
    fun `страницы упорядочиваются по index, а не по порядку в разметке`() {
        val refs = ChapterPlan.toPageRefs(
            "ch12", chapterUrl,
            listOf(img(2, "https://cdn/c.jpg"), img(0, "https://cdn/a.jpg"), img(1, "https://cdn/b.jpg")),
        )

        assertEquals(
            listOf("https://cdn/a.jpg", "https://cdn/b.jpg", "https://cdn/c.jpg"),
            refs.map(PageRef::uri),
        )
    }

    @Test
    fun `дубль url не даёт дубль страницы`() {
        val refs = ChapterPlan.toPageRefs(
            "ch12", chapterUrl,
            listOf(img(0, "https://cdn/a.jpg"), img(1, "https://cdn/a.jpg"), img(2, "https://cdn/b.jpg")),
        )

        assertEquals(2, refs.size)
        assertEquals(listOf(0, 1), refs.map(PageRef::index))
    }

    @Test
    fun `id страницы уникален внутри главы`() {
        val refs = ChapterPlan.toPageRefs(
            "ch12", chapterUrl,
            (0..9).map { img(it, "https://cdn/$it.jpg") },
        )

        assertEquals(refs.size, refs.map(PageRef::id).toSet().size)
    }

    @Test
    fun `размеры страницы прокидываются, когда парсер их знает`() {
        val refs = ChapterPlan.toPageRefs(
            "ch12", chapterUrl,
            listOf(ImagePage(index = 0, url = "https://cdn/a.jpg", width = 800, height = 1200)),
        )

        assertEquals(800, refs[0].width)
        assertEquals(1200, refs[0].height)
    }

    @Test
    fun `origin вычисляется без пути и запроса`() {
        assertEquals("https://example.org", ChapterPlan.originOf("https://example.org/a/b?c=1#d"))
        assertEquals("http://a.test", ChapterPlan.originOf("http://a.test"))
        assertNull(ChapterPlan.originOf("/relative/path"))
        assertNull(ChapterPlan.originOf("https:///nohost"))
    }

    @Test
    fun `соседи главы и края списка`() {
        val ids = listOf("c1", "c2", "c3")

        assertEquals("c3", ChapterPlan.neighbour(ids, "c2", +1))
        assertEquals("c1", ChapterPlan.neighbour(ids, "c2", -1))
        assertNull(ChapterPlan.neighbour(ids, "c3", +1))
        assertNull(ChapterPlan.neighbour(ids, "c1", -1))
        assertNull(ChapterPlan.neighbour(ids, "нет-такой", +1))
    }

    @Test
    fun `пустая глава даёт пустой список, а не исключение`() {
        assertTrue(ChapterPlan.toPageRefs("ch12", chapterUrl, emptyList()).isEmpty())
    }
}

/**
 * Имя файла страницы. Логика та же, что в `WebMangaPageSource`, и
 * причина у неё та же, что у ключа дискового кеша читалки: один URL с
 * разным `Referer` — разное тело ответа.
 */
class PageFileNameTest {

    private fun name(uri: String, headers: Map<String, String>): String {
        val keyed = headers
            .filterKeys { it.lowercase() in setOf("referer", "origin", "cookie") }
            .map { (n, v) -> n.lowercase() + "=" + v }
            .sorted()
            .joinToString("&")
        return java.security.MessageDigest.getInstance("SHA-1")
            .digest((uri + "\n" + keyed).toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `разный referer при одном url даёт разные файлы`() {
        val a = name("https://cdn/a.jpg", mapOf("Referer" to "https://one.test/ch-1"))
        val b = name("https://cdn/a.jpg", mapOf("Referer" to "https://two.test/ch-1"))

        assertTrue("иначе заглушка с 403 осела бы вместо страницы", a != b)
    }

    @Test
    fun `регистр и порядок заголовков не плодят разные файлы`() {
        val a = name("https://cdn/a.jpg", linkedMapOf("Referer" to "https://x.test/", "Origin" to "https://x.test"))
        val b = name("https://cdn/a.jpg", linkedMapOf("origin" to "https://x.test", "REFERER" to "https://x.test/"))

        assertEquals(a, b)
    }

    @Test
    fun `заголовки вне списка не влияют на имя`() {
        // Иначе имя менялось бы от запроса к запросу и файл никогда
        // не переиспользовался бы — кеш есть, попаданий нет.
        val a = name("https://cdn/a.jpg", mapOf("Referer" to "https://x.test/", "User-Agent" to "UA-1"))
        val b = name("https://cdn/a.jpg", mapOf("Referer" to "https://x.test/", "X-Request-Id" to "42"))

        assertEquals(a, b)
    }

    @Test
    fun `один url в двух главах это один файл`() {
        val headers = mapOf("Referer" to "https://x.test/ch-1")
        assertEquals(name("https://cdn/a.jpg", headers), name("https://cdn/a.jpg", headers))
    }
}
