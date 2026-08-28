package moe.scenesearch.core.tracemoe

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import moe.scenesearch.api.ImageQuery
import moe.scenesearch.api.MediaKind
import moe.scenesearch.api.QuotaInfo
import moe.scenesearch.api.SceneIdentity
import moe.scenesearch.api.SceneSearchError
import moe.scenesearch.api.SceneSearchProvider
import moe.scenesearch.api.TitleSet
import moe.scenesearch.core.http.await
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Поиск сцены аниме через api.trace.moe.
 * Ключ передаётся только заголовком x-trace-key, query-параметр key на сервере отключён.
 * Гостю доступна ограниченная суточная квота на IP при concurrency равном единице.
 */
class TraceMoeProvider(
    private val client: OkHttpClient = OkHttpClient(),
    private val apiKey: String? = null,
    private val endpoint: String = "https://api.trace.moe",
    concurrency: Int = 1,
) : SceneSearchProvider {

    override val id: String = "trace.moe"
    override val supports: Set<MediaKind> = setOf(MediaKind.ANIME)

    private val gate = Semaphore(if (concurrency < 1) 1 else concurrency)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun identify(query: ImageQuery): List<SceneIdentity> {
        if (query.kind !in supports) return emptyList()
        if (query.bytes.isEmpty()) throw SceneSearchError.BadImage("empty payload")
        if (query.bytes.size > MAX_BYTES) throw SceneSearchError.TooLarge(query.bytes.size)

        val builder = (endpoint + "/search").toHttpUrl().newBuilder()
        builder.addQueryParameter("anilistInfo", "")
        if (query.cutBorders) builder.addQueryParameter("cutBorders", "")
        val filter = query.filterByAnilistId
        if (filter != null) builder.addQueryParameter("anilistID", filter.toString())

        val requestBuilder = Request.Builder()
            .url(builder.build())
            .post(query.bytes.toRequestBody(query.mime.toMediaType()))
        if (apiKey != null) requestBuilder.header("x-trace-key", apiKey)
        val request = requestBuilder.build()

        return gate.withPermit {
            client.newCall(request).await().use { response ->
                val body = response.body?.string().orEmpty()
                throwIfFailed(response.code, body)
                val parsed = runCatching { json.decodeFromString<TraceMoeResponse>(body) }
                    .getOrElse { throw SceneSearchError.Unavailable("malformed response", it) }
                if (parsed.error.isNotBlank()) throw SceneSearchError.Unavailable(parsed.error)
                parsed.result.map { toIdentity(it) }.sortedByDescending { it.similarity }
            }
        }
    }

    override suspend fun quota(): QuotaInfo? {
        val requestBuilder = Request.Builder().url(endpoint + "/me")
        if (apiKey != null) requestBuilder.header("x-trace-key", apiKey)
        return runCatching {
            client.newCall(requestBuilder.build()).await().use { response ->
                if (!response.isSuccessful) return null
                val me = json.decodeFromString<TraceMoeMe>(response.body?.string().orEmpty())
                QuotaInfo(me.id, me.priority, me.concurrency, me.quota, me.quotaUsed)
            }
        }.getOrNull()
    }

    private fun throwIfFailed(code: Int, body: String) {
        when (code) {
            in 200..299 -> return
            400 -> throw SceneSearchError.BadImage("trace.moe rejected the image")
            402 -> throw SceneSearchError.QuotaDepleted(0, 0)
            403 -> throw SceneSearchError.Unauthorized()
            413 -> throw SceneSearchError.TooLarge(-1)
            429 -> throw SceneSearchError.Concurrency()
            else -> throw SceneSearchError.Unavailable("http " + code + ": " + body.take(200))
        }
    }

    private fun toIdentity(r: TraceMoeResult): SceneIdentity {
        val parsed = parseEpisode(r.episode)
        val al = r.anilist
        return SceneIdentity(
            providerId = id,
            anilistId = al?.id?.takeIf { it > 0 },
            malId = al?.idMal,
            titles = TitleSet(
                romaji = al?.title?.romaji,
                english = al?.title?.english,
                native = al?.title?.native,
                synonyms = al?.synonyms.orEmpty(),
            ),
            episode = parsed.first,
            episodeRange = parsed.second,
            fromSec = r.from,
            toSec = r.to,
            similarity = r.similarity,
            isAdult = al?.isAdult ?: false,
            previewImageUrl = r.image,
            previewVideoUrl = r.video,
            rawFilename = r.filename.ifBlank { null },
        )
    }

    companion object {
        const val MAX_BYTES: Int = 25 * 1024 * 1024

        /**
         * Поле episode приходит из имени файла: число, строка вида два дефис три,
         * массив или null. Молча это глотаем, а не падаем.
         */
        internal fun parseEpisode(el: JsonElement?): Pair<Int?, IntRange?> {
            if (el == null || el is JsonNull) return null to null
            if (el is JsonArray) {
                val nums = el.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.toIntOrNull() }
                if (nums.isEmpty()) return null to null
                val lo = nums.min()
                val hi = nums.max()
                return lo to (lo..hi)
            }
            if (el is JsonPrimitive) {
                val raw = el.contentOrNull?.trim() ?: return null to null
                val single = raw.toIntOrNull()
                if (single != null) return single to null
                val ranged = Regex("""(\d+)\s*[-~]\s*(\d+)""").find(raw)
                if (ranged != null) {
                    val a = ranged.groupValues[1].toInt()
                    val b = ranged.groupValues[2].toInt()
                    val lo = if (a < b) a else b
                    val hi = if (a < b) b else a
                    return lo to (lo..hi)
                }
                return Regex("""\d+""").find(raw)?.value?.toIntOrNull() to null
            }
            return null to null
        }
    }
}
