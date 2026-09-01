package dev.takami.source.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Что источнику нужно от сети. Вынесено интерфейсом, потому что читалка
 * требует от `open()` **стабильный, полностью записанный файл** — а это
 * значит скачивание во временный файл и атомарный переезд на место.
 * Тестам же нужна возможность подсунуть локальные байты без сети.
 */
interface PageFetcher {

    /** HTML страницы. */
    suspend fun html(url: String, headers: Map<String, String> = emptyMap()): String

    /**
     * Скачивает изображение в [target]. Реализация обязана обеспечить,
     * что по возвращении файл записан целиком: читалка декодирует его
     * дважды и делает это уже после завершения потока.
     */
    suspend fun download(
        url: String,
        headers: Map<String, String>,
        target: File,
        onProgress: (bytes: Long, total: Long?) -> Unit = { _, _ -> },
    )
}

/**
 * Боевая реализация на OkHttp-клиенте автопарсера: он уже умеет
 * вежливость к хостам, куки и распознавание проверок браузера.
 */
class HttpPageFetcher(
    private val client: core.net.HttpClient,
) : PageFetcher {

    override suspend fun html(url: String, headers: Map<String, String>): String {
        val merged = client.browserHeaders(referer = url) + headers
        val response = client.get(url, merged)
        if (!response.isSuccess) {
            throw PageSourceException("HTTP ${response.code} при загрузке $url")
        }
        return response.body
    }

    override suspend fun download(
        url: String,
        headers: Map<String, String>,
        target: File,
        onProgress: (Long, Long?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        /*
         * Пишем в .part и переименовываем. Без этого оборванная закачка
         * оставляет на диске обрезанный файл, который читалка примет за
         * готовый и покажет половину страницы — а при следующем открытии
         * возьмёт его же из кеша и покажет ту же половину снова.
         */
        val part = File(target.parentFile, target.name + ".part")
        part.parentFile?.mkdirs()
        part.delete()

        val merged = client.browserHeaders(referer = headers["Referer"]) + headers

        /*
         * Именно `client.download`, а не `get`: `HttpResponse.body` —
         * это String, декодированная по charset ответа, и картинка
         * через неё портится необратимо. Плюс поток не держит всё тело
         * в памяти и не режется по maxBodyBytes.
         */
        try {
            part.outputStream().buffered().use { out ->
                client.download(url, merged, out, onProgress)
            }
        } catch (e: Throwable) {
            part.delete()   // битый огрызок не должен дожить до renameTo
            throw if (e is PageSourceException) e
            else PageSourceException("не удалось загрузить страницу: ${e.message}", e)
        }

        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
        Unit
    }
}

class PageSourceException(message: String, cause: Throwable? = null) : Exception(message, cause)
