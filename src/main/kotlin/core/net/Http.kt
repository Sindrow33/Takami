package core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

/* =====================================================================
 * 1. МОДЕЛЬ ОТВЕТА
 * ===================================================================== */

data class HttpResponse(
    val code: Int,
    val body: String,
    val finalUrl: String,
    val headers: Map<String, String>,
    val contentType: String?,
    val elapsedMillis: Long,
    val fromNetwork: Boolean,
    /** Тело обрезано по лимиту — валидатору важно не считать это поломкой. */
    val truncated: Boolean = false,
) {
    val isSuccess: Boolean get() = code in 200..299
    val isJson: Boolean get() = contentType?.contains("json", ignoreCase = true) == true
    val isHtml: Boolean get() = contentType?.contains("html", ignoreCase = true) != false
    val bodySize: Int get() = body.length
}

/** Разделение ошибок по типам — основа для BreakageClassifier. */
sealed class HttpError(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class Timeout(cause: Throwable?) : HttpError("таймаут соединения", cause)
    class NoNetwork(cause: Throwable?) : HttpError("нет сети", cause)
    class Dns(val host: String, cause: Throwable?) : HttpError("хост не резолвится: $host", cause)
    class Tls(cause: Throwable?) : HttpError("ошибка TLS", cause)
    class TooLarge(val size: Long) : HttpError("тело слишком большое: $size байт")
    class BadStatus(val code: Int, val bodyPreview: String) : HttpError("HTTP $code")
    class RateLimited(val retryAfterMillis: Long?) : HttpError("слишком много запросов")
    class Cancelled : HttpError("запрос отменён")
    class Unknown(cause: Throwable?) : HttpError("сетевая ошибка: ${cause?.message}", cause)

    /** Стоит ли повторять. 403/404 повторять бессмысленно, таймаут — стоит. */
    val isTransient: Boolean get() = this is Timeout || this is NoNetwork || this is Unknown ||
            (this is BadStatus && code in RETRYABLE_CODES)

    companion object { private val RETRYABLE_CODES = setOf(502, 503, 504, 408, 425) }
}

/* =====================================================================
 * 2. ИНТЕРФЕЙС
 * ===================================================================== */

interface HttpClient {
    /** Минимальный набор: User-Agent, Accept. Для API и простых страниц. */
    fun defaultHeaders(): Map<String, String>

    /** Полный браузерный набор с Referer и Sec-Fetch-*. */
    fun browserHeaders(referer: String? = null, origin: String? = null): Map<String, String>

    suspend fun get(url: String, headers: Map<String, String> = defaultHeaders()): HttpResponse

    suspend fun post(
        url: String,
        body: String,
        contentType: String = "application/x-www-form-urlencoded",
        headers: Map<String, String> = defaultHeaders(),
    ): HttpResponse

    /**
     * Условный запрос для канареек: если сайт ответил 304, страница
     * не менялась и проверять нечего — экономим и трафик, и разбор.
     */
    suspend fun getIfChanged(url: String, etag: String?, lastModified: String?,
                             headers: Map<String, String> = defaultHeaders()): HttpResponse?

    /** Только заголовки — узнать размер и тип, не скачивая тело. */
    suspend fun head(url: String, headers: Map<String, String> = defaultHeaders()): HttpResponse

    fun cookiesFor(host: String): List<String>
    suspend fun clearCookies(host: String)
}

/* =====================================================================
 * 3. КУКИ
 * ===================================================================== */

/**
 * Реализация хранилища появится в слое персистентности; здесь только
 * контракт. Сохранять куки между запусками обязательно: после того как
 * пользователь один раз прошёл проверку браузера, полученная кука живёт
 * часами. Потеряв её при перезапуске, мы отправим человека проходить
 * проверку заново — на ровном месте.
 */
interface CookieStore {
    fun load(host: String): List<Cookie>
    fun save(host: String, cookies: List<Cookie>)
    fun clear(host: String)
    fun clearExpired(now: Long = System.currentTimeMillis())
}

/** Память — на случай тестов и до подключения диска. */
class InMemoryCookieStore : CookieStore {
    private val map = HashMap<String, MutableList<Cookie>>()

    @Synchronized
    override fun load(host: String): List<Cookie> {
        val now = System.currentTimeMillis()
        return map[host]?.filter { it.expiresAt > now } ?: emptyList()
    }

    @Synchronized
    override fun save(host: String, cookies: List<Cookie>) {
        val list = map.getOrPut(host) { mutableListOf() }
        for (c in cookies) {
            list.removeAll { it.name == c.name && it.path == c.path }
            list += c
        }
    }

    @Synchronized
    override fun clear(host: String) { map.remove(host) }

    @Synchronized
    override fun clearExpired(now: Long) {
        for ((_, list) in map) list.removeAll { it.expiresAt <= now }
    }
}

internal class PersistentCookieJar(private val store: CookieStore) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        store.save(url.host.removePrefix("www."), cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host.removePrefix("www.")
        return store.load(host).filter { it.matches(url) }
    }
}

/* =====================================================================
 * 4. ВЕЖЛИВОСТЬ
 * ===================================================================== */

/**
 * Минимальный интервал между запросами к одному хосту и потолок
 * параллельности. Это не только этика: агрессивный клиент на мобильном
 * IP получает 429 за минуту, после чего вся диагностика начинает видеть
 * несуществующие поломки вёрстки.
 */
class HostRateLimiter(
    private val minGapMillis: Long = 350,
    private val maxConcurrentPerHost: Int = 2,
) {
    private val lock = Mutex()
    private val lastAt = HashMap<String, Long>()
    private val active = HashMap<String, Int>()
    private val backoffUntil = HashMap<String, Long>()

    suspend fun acquire(host: String) {
        while (true) {
            val waitFor = lock.withLock {
                val now = System.currentTimeMillis()
                backoffUntil[host]?.let { if (it > now) return@withLock it - now }
                if ((active[host] ?: 0) >= maxConcurrentPerHost) return@withLock CONCURRENCY_POLL
                val gap = now - (lastAt[host] ?: 0L)
                if (gap < minGapMillis) return@withLock minGapMillis - gap
                lastAt[host] = now
                active[host] = (active[host] ?: 0) + 1
                0L
            }
            if (waitFor == 0L) return
            delay(waitFor)
        }
    }

    suspend fun release(host: String) = lock.withLock {
        active[host] = ((active[host] ?: 1) - 1).coerceAtLeast(0)
    }

    /** Сервер попросил подождать — уважаем и запоминаем. */
    suspend fun penalize(host: String, millis: Long) = lock.withLock {
        val until = System.currentTimeMillis() + millis.coerceIn(1_000, MAX_BACKOFF)
        backoffUntil[host] = maxOf(backoffUntil[host] ?: 0L, until)
    }

    companion object {
        private const val CONCURRENCY_POLL = 60L
        private const val MAX_BACKOFF = 10 * 60_000L
    }
}

/* =====================================================================
 * 5. РЕАЛИЗАЦИЯ
 * ===================================================================== */

class OkHttpClientAdapter(
    cookieStore: CookieStore = InMemoryCookieStore(),
    private val limiter: HostRateLimiter = HostRateLimiter(),
    private val userAgent: String = DESKTOP_UA,
    private val acceptLanguage: String = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
    private val maxBodyBytes: Long = 8L * 1024 * 1024,
    private val maxRetries: Int = 2,
) : HttpClient {

    private val jar = PersistentCookieJar(cookieStore)
    private val store = cookieStore

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .cookieJar(jar)
        .connectionPool(ConnectionPool(6, 5, TimeUnit.MINUTES))
        .build()

    /* --- заголовки --------------------------------------------------- */

    override fun defaultHeaders(): Map<String, String> = mapOf(
        "User-Agent" to userAgent,
        "Accept" to "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
        "Accept-Language" to acceptLanguage,
        "Accept-Encoding" to "gzip",
    )

    override fun browserHeaders(referer: String?, origin: String?): Map<String, String> = buildMap {
        put("User-Agent", userAgent)
        put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
        put("Accept-Language", acceptLanguage)
        put("Accept-Encoding", "gzip")
        put("Upgrade-Insecure-Requests", "1")
        put("Sec-Fetch-Dest", "document")
        put("Sec-Fetch-Mode", "navigate")
        put("Sec-Fetch-Site", if (referer == null) "none" else "same-origin")
        put("Sec-Fetch-User", "?1")
        put("Cache-Control", "max-age=0")
        referer?.let { put("Referer", it) }
        origin?.let { put("Origin", it) }
    }

    /* --- запросы ----------------------------------------------------- */

    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse =
        execute(buildRequest(url, headers).build())

    override suspend fun post(
        url: String, body: String, contentType: String, headers: Map<String, String>,
    ): HttpResponse = execute(
        buildRequest(url, headers)
            .post(RequestBody.create(MediaType.parse(contentType), body))
            .build()
    )

    override suspend fun getIfChanged(
        url: String, etag: String?, lastModified: String?, headers: Map<String, String>,
    ): HttpResponse? {
        val req = buildRequest(url, headers).apply {
            etag?.let { header("If-None-Match", it) }
            lastModified?.let { header("If-Modified-Since", it) }
        }.build()
        val resp = execute(req, allowNotModified = true)
        return if (resp.code == 304) null else resp
    }

    override suspend fun head(url: String, headers: Map<String, String>): HttpResponse =
        execute(buildRequest(url, headers).head().build())

    override fun cookiesFor(host: String): List<String> =
        store.load(host.removePrefix("www.")).map { "${it.name}=${it.value}" }

    override suspend fun clearCookies(host: String) {
        store.clear(host.removePrefix("www."))
    }

    /* --- ядро -------------------------------------------------------- */

    private fun buildRequest(url: String, headers: Map<String, String>): Request.Builder {
        val builder = Request.Builder().url(url)
        for ((k, v) in headers) if (v.isNotBlank()) builder.header(k, v)
        return builder
    }

    private suspend fun execute(request: Request, allowNotModified: Boolean = false): HttpResponse {
        val host = request.url().host()
        var attempt = 0
        var lastError: HttpError? = null

        while (attempt <= maxRetries) {
            attempt++
            limiter.acquire(host)
            val startedAt = System.currentTimeMillis()
            try {
                val response = withContext(Dispatchers.IO) { client.newCall(request).await() }
                response.use { r ->
                    val code = r.code()

                    if (code == 429 || code == 503) {
                        val retryAfter = parseRetryAfter(r.header("Retry-After"))
                        limiter.penalize(host, retryAfter ?: DEFAULT_PENALTY)
                        // Не ретраим здесь: это работа для лестницы загрузки,
                        // которая может подняться до рендера, а не долбить тем же.
                        throw HttpError.RateLimited(retryAfter)
                    }

                    if (code == 304 && allowNotModified) {
                        return HttpResponse(304, "", r.request().url().toString(),
                            headersOf(r), null, System.currentTimeMillis() - startedAt, true)
                    }

                    val contentLength = r.body()?.contentLength() ?: -1L
                    if (contentLength > maxBodyBytes) throw HttpError.TooLarge(contentLength)

                    val (text, truncated) = readBounded(r)

                    return HttpResponse(
                        code = code,
                        body = text,
                        finalUrl = r.request().url().toString(),
                        headers = headersOf(r),
                        contentType = r.header("Content-Type"),
                        elapsedMillis = System.currentTimeMillis() - startedAt,
                        fromNetwork = r.networkResponse() != null,
                        truncated = truncated,
                    )
                }
            } catch (e: Throwable) {
                val mapped = mapError(e, host)
                lastError = mapped
                if (!mapped.isTransient || attempt > maxRetries) throw mapped
                // Джиттер обязателен: без него все отложенные задачи
                // просыпаются одновременно и создают собственный всплеск.
                delay(backoffMillis(attempt))
            } finally {
                limiter.release(host)
            }
        }
        throw lastError ?: HttpError.Unknown(null)
    }

    /**
     * Читаем с потолком. Без него страница с бесконечным чанк-ответом
     * (или просто дамп базы вместо HTML) выедает всю память процесса.
     */
    private fun readBounded(r: Response): Pair<String, Boolean> {
        val body = r.body() ?: return "" to false
        val source = body.source()
        source.request(maxBodyBytes + 1)
        val buffer = source.buffer()
        val truncated = buffer.size() > maxBodyBytes
        val bytes = buffer.snapshot(minOf(buffer.size(), maxBodyBytes).toInt())
        val charset = body.contentType()?.charset() ?: Charsets.UTF_8
        return bytes.string(charset) to truncated
    }

    private fun headersOf(r: Response): Map<String, String> =
        r.headers().names().associateWith { r.header(it).orEmpty() }

    private fun mapError(e: Throwable, host: String): HttpError = when (e) {
        is HttpError -> e
        is SocketTimeoutException -> HttpError.Timeout(e)
        is UnknownHostException -> HttpError.Dns(host, e)
        is SSLException -> HttpError.Tls(e)
        is java.util.concurrent.CancellationException -> HttpError.Cancelled()
        is IOException ->
            if (e.message?.contains("Canceled", true) == true) HttpError.Cancelled()
            else HttpError.NoNetwork(e)
        else -> HttpError.Unknown(e)
    }

    private fun backoffMillis(attempt: Int): Long {
        val base = 400L * (1 shl (attempt - 1))
        return base + Random.nextLong(0, base / 2)
    }

    private fun parseRetryAfter(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        value.toLongOrNull()?.let { return it * 1000 }
        return runCatching {
            val date = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
                .parse(value)
            (date.time - System.currentTimeMillis()).coerceAtLeast(0)
        }.getOrNull()
    }

    companion object {
        const val DESKTOP_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val DEFAULT_PENALTY = 30_000L
    }
}

/** Мост между колбэками OkHttp и корутинами, с отменой. */
private suspend fun Call.await(): Response = suspendCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
        override fun onFailure(call: Call, e: IOException) {
            if (call.isCanceled()) cont.resumeWithException(HttpError.Cancelled())
            else cont.resumeWithException(e)
        }
    })
}

/* =====================================================================
 * 6. РАСПОЗНАВАНИЕ ПРОВЕРОК
 * ===================================================================== */

enum class ChallengeKind { BROWSER_CHECK, CAPTCHA, LOGIN_WALL, GEO_BLOCK, RATE_LIMIT_PAGE }

data class ChallengeInfo(val kind: ChallengeKind, val marker: String, val provider: String?)

/**
 * Задача ровно одна: понять, что перед нами не контент, и честно сказать
 * об этом. Ничего не обходится и не решается — при обнаружении проверки
 * страница отдаётся пользователю в браузере. Без этого распознавания
 * классификатор принял бы страницу проверки за смену вёрстки и запустил
 * бесполезный ремонт по HTML капчи.
 */
class ChallengeDetector {

    fun detect(html: String, statusCode: Int = 200): ChallengeInfo? {
        if (html.length > SCAN_LIMIT) return detectIn(html.substring(0, SCAN_LIMIT), statusCode)
        return detectIn(html, statusCode)
    }

    private fun detectIn(html: String, statusCode: Int): ChallengeInfo? {
        val low = html.lowercase()

        // Страницы проверки почти всегда короткие. Длинный документ
        // с одним совпадением слова — скорее статья про капчу.
        val short = html.length < SHORT_PAGE

        for ((marker, info) in BROWSER_CHECK) {
            if (low.contains(marker)) return ChallengeInfo(ChallengeKind.BROWSER_CHECK, marker, info)
        }
        for ((marker, info) in CAPTCHA) {
            if (low.contains(marker)) return ChallengeInfo(ChallengeKind.CAPTCHA, marker, info)
        }
        if (short) {
            for (marker in GEO) if (low.contains(marker))
                return ChallengeInfo(ChallengeKind.GEO_BLOCK, marker, null)
            for (marker in RATE) if (low.contains(marker))
                return ChallengeInfo(ChallengeKind.RATE_LIMIT_PAGE, marker, null)
        }
        if (statusCode == 401 || (short && LOGIN.any { low.contains(it) }))
            return ChallengeInfo(ChallengeKind.LOGIN_WALL, "login", null)

        // Пустой каркас без контента при коде 200 — тоже сигнал.
        if (statusCode == 200 && html.length < TINY_PAGE && low.contains("<script"))
            return ChallengeInfo(ChallengeKind.BROWSER_CHECK, "empty-shell", null)

        return null
    }

    private companion object {
        const val SCAN_LIMIT = 60_000
        const val SHORT_PAGE = 20_000
        const val TINY_PAGE = 2_500

        val BROWSER_CHECK = listOf(
            "just a moment" to "cloudflare",
            "checking your browser" to "cloudflare",
            "cf-browser-verification" to "cloudflare",
            "cf_chl_opt" to "cloudflare",
            "проверка браузера" to null,
            "ddos-guard" to "ddos-guard",
            "__ddg" to "ddos-guard",
            "qrator" to "qrator",
            "incapsula incident id" to "imperva",
            "_incapsula_resource" to "imperva",
            "please wait while we verify" to null,
        )

        val CAPTCHA = listOf(
            "g-recaptcha" to "recaptcha",
            "recaptcha/api.js" to "recaptcha",
            "hcaptcha.com/1/api.js" to "hcaptcha",
            "h-captcha" to "hcaptcha",
            "smartcaptcha" to "yandex",
            "turnstile" to "cloudflare",
            "подтвердите, что вы не робот" to null,
            "i am not a robot" to null,
        )

        val GEO = listOf(
            "not available in your country", "недоступно в вашей стране",
            "geo-restricted", "content is unavailable in your region",
        )

        val RATE = listOf(
            "too many requests", "слишком много запросов", "rate limit exceeded",
        )

        val LOGIN = listOf(
            "please log in", "sign in to continue", "войдите, чтобы продолжить",
            "требуется авторизация",
        )
    }
}

/* =====================================================================
 * 7. АБСОЛЮТНЫЕ ССЫЛКИ
 * ===================================================================== */

/**
 * Jsoup умеет absUrl, но только когда узел взят из документа с baseUri.
 * Значения, вытащенные из JSON или из скриптов, приходят без контекста —
 * им нужен независимый резолвер.
 */
object UrlTools {

    fun absolutize(url: String, base: String): String {
        val u = url.trim()
        if (u.isEmpty()) return u
        return when {
            u.startsWith("http://") || u.startsWith("https://") -> u
            u.startsWith("//") -> (schemeOf(base) ?: "https") + ":" + u
            u.startsWith("data:") || u.startsWith("blob:") -> u
            else -> runCatching { java.net.URI(base).resolve(u).toString() }.getOrDefault(u)
        }
    }

    fun schemeOf(url: String): String? = runCatching { java.net.URI(url).scheme }.getOrNull()

    /** Одна ли это площадка. Нужно для фильтрации картинок по хосту. */
    fun sameSite(a: String, b: String): Boolean {
        val ha = registrable(a) ?: return false
        val hb = registrable(b) ?: return false
        return ha == hb
    }

    /** Грубая «регистрируемая» часть: последние два уровня домена. */
    private fun registrable(url: String): String? {
        val host = runCatching { java.net.URI(url).host }.getOrNull() ?: return null
        val parts = host.removePrefix("www.").split('.')
        return if (parts.size <= 2) host.removePrefix("www.")
        else parts.takeLast(2).joinToString(".")
    }

    fun looksLikeUrl(value: String): Boolean {
        val v = value.trim()
        if (v.length < 4 || v.contains(' ')) return false
        return v.startsWith("http://") || v.startsWith("https://") ||
               v.startsWith("//") || v.startsWith("/")
    }
}
