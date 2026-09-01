package core

import core.net.OkHttpClientAdapter
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Сжатый ответ обязан приезжать распакованным.
 *
 * Регрессия дорогая и незаметная: клиент руками ставил
 * `Accept-Encoding: gzip`, чем ОТКЛЮЧАЛ прозрачную распаковку в OkHttp
 * — тело оставалось архивом, декодировалось по charset в мусор, и
 * разбор любого сайта с gzip возвращал ноль элементов. Снаружи это
 * выглядело как «сайт отдаёт пустую страницу», а диагностика списывала
 * всё на скрипты. Тест держит и причину (заголовок не ставим руками), и
 * следствие (текст читается).
 */
class GzipBodyTest {

    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { server.shutdown() }

    private val page = "<html><body><article class=\"post\"><h2>Тайтл</h2></article></body></html>"

    private fun gzipped(text: String): Buffer {
        val out = Buffer()
        GzipSink(out).buffer().use { it.writeUtf8(text) }
        return out
    }

    @Test
    fun gzipBodyIsDecompressed() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setHeader("Content-Encoding", "gzip")
                .setBody(gzipped(page)),
        )

        val client = OkHttpClientAdapter()
        val response = runBlocking { client.get(server.url("/").toString()) }

        assertEquals(page, response.body)
        assertTrue("тело не распаковано", response.body.contains("Тайтл"))
    }

    @Test
    fun clientDoesNotSetAcceptEncoding() {
        // Именно ручной заголовок и отключал распаковку: OkHttp считает,
        // что сжатием управляет вызывающий, и отдаёт тело как есть.
        assertTrue(
            "Accept-Encoding в defaultHeaders вернёт баг",
            OkHttpClientAdapter().defaultHeaders().keys.none { it.equals("Accept-Encoding", true) },
        )
        assertTrue(
            "Accept-Encoding в browserHeaders вернёт баг",
            OkHttpClientAdapter().browserHeaders().keys.none { it.equals("Accept-Encoding", true) },
        )
    }

    @Test
    fun compressedBodyIsNotPassedOffAsText() {
        // Если сжатие всё же не сняли, ошибка обязана быть громкой:
        // тихий мусор в теле стоил дня разбирательств.
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody(gzipped(page)),
        )

        val client = OkHttpClientAdapter()
        val thrown = runCatching {
            runBlocking { client.get(server.url("/").toString()) }
        }.exceptionOrNull()

        assertTrue("ожидали явную ошибку, получили $thrown", thrown is core.net.HttpError.Malformed)
    }
}
