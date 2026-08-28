package moe.scenesearch.core.saucenao

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Манга и артворки: trace.moe их не индексирует.
 * Индексы баз: 37 это MangaDex, 36 Madokami, 21 Anime, 999 все сразу.
 */
class SauceNaoProvider(
    private val client: OkHttpClient = OkHttpClient(),
    private val apiKey: String,
    private val endpoint: String = "https://saucenao.com/search.php",
    private val dbIndex: Int = 999,
    private val numRes: Int = 5,
    private val minSimilarity: Double = 0.60,
) : SceneSearchProvider {

    override val id: String = "saucenao"
    override val supports: Set<MediaKind> = setOf(MediaKind.MANGA, MediaKind.ANIME)

    private val gate = Semaphore(1)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var lastQuota: QuotaInfo? = null

    override suspend fun identify(query: ImageQuery): List<SceneIdentity> {
        if (query.kind !in supports) return emptyList()
        if (query.bytes.isEmpty()) throw SceneSearchError.BadImage("empty payload")

        val url = endpoint.toHttpUrl().newBuilder()
            .addQueryParameter("output_type", "2")
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("db", dbIndex.toString())
            .addQueryParameter("numres", numRes.toString())
            .build()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "image.jpg", query.bytes.toRequestBody(query.mime.toMediaType()))
            .build()

        return gate.withPermit {
            client.newCall(Request.Builder().url(url).post(body).build()).await().use { response ->
                val text = response.body?.string().orEmpty()
                when (response.code) {
                    in 200..299 -> Unit
                    403 -> throw SceneSearchError.Unauthorized()
                    413 -> throw SceneSearchError.TooLarge(query.bytes.size)
                    429 -> throw SceneSearchError.QuotaDepleted(0, 0)
                    else -> throw SceneSearchError.Unavailable("http " + response.code)
                }
                val root = runCatching { json.parseToJsonElement(text).jsonObject }
                    .getOrElse { throw SceneSearchError.Unavailable("malformed response", it) }
                readQuota(root)
                val results = root["results"] as? JsonArray ?: JsonArray(emptyList())
                results.mapNotNull { toIdentity(it.jsonObject) }
                    .filter { it.similarity >= minSimilarity }
                    .sortedByDescending { it.similarity }
            }
        }
    }

    override suspend fun quota(): QuotaInfo? = lastQuota

    private fun readQuota(root: JsonObject) {
        val header = root["header"]?.jsonObject ?: return
        val left = header["long_remaining"]?.jsonPrimitive?.intOrNull ?: return
        val limit = header["long_limit"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: left
        val used = limit - left
        lastQuota = QuotaInfo(id = id, quota = limit, quotaUsed = if (used < 0) 0 else used)
    }

    private fun toIdentity(obj: JsonObject): SceneIdentity? {
        val header = obj["header"]?.jsonObject ?: return null
        val data = obj["data"]?.jsonObject ?: return null
        val raw = header["similarity"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return null
        val similarity = raw / 100.0

        val title = data["source"]?.jsonPrimitive?.contentOrNull
            ?: data["title"]?.jsonPrimitive?.contentOrNull
            ?: data["eng_name"]?.jsonPrimitive?.contentOrNull
            ?: return null

        val part = data["part"]?.jsonPrimitive?.contentOrNull
        val partNumber = if (part == null) null else Regex("""\d+""").find(part)?.value?.toIntOrNull()

        return SceneIdentity(
            providerId = id,
            anilistId = data["anilist_id"]?.jsonPrimitive?.intOrNull,
            malId = data["mal_id"]?.jsonPrimitive?.intOrNull,
            titles = TitleSet(
                romaji = title,
                english = data["eng_name"]?.jsonPrimitive?.contentOrNull,
                native = data["jp_name"]?.jsonPrimitive?.contentOrNull,
                synonyms = listOfNotNull(data["title"]?.jsonPrimitive?.contentOrNull),
            ),
            episode = partNumber,
            chapter = part,
            fromSec = parseEstTime(data["est_time"]?.jsonPrimitive?.contentOrNull),
            similarity = similarity,
            previewImageUrl = header["thumbnail"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun parseEstTime(raw: String?): Double {
        if (raw == null) return 0.0
        val m = Regex("""(\d+):(\d{2}):(\d{2})""").find(raw) ?: return 0.0
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        val s = m.groupValues[3].toInt()
        return (h * 3600 + min * 60 + s).toDouble()
    }
}
