package dev.anime.player.skip.net

import dev.anime.player.skip.SkipProvider
import dev.anime.player.skip.SkipSegment
import dev.anime.player.skip.SkipType
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class AniSkipInterval(
    val startTime: Double = 0.0,
    val endTime: Double = 0.0,
)

@Serializable
internal data class AniSkipResult(
    val interval: AniSkipInterval = AniSkipInterval(),
    val skipType: String = "",
    val skipId: String? = null,
    val episodeLength: Double? = null,
)

@Serializable
internal data class AniSkipResponse(
    val found: Boolean = false,
    val results: List<AniSkipResult> = emptyList(),
    val message: String? = null,
    val statusCode: Int? = null,
)

/**
 * Реальный провайдер skip-таймингов через публичный AniSkip API
 * (https://api.aniskip.com/api-docs). Индексирует эпизоды по MAL id,
 * без него результата не будет — см. [AniListAwareSkipProvider] для
 * автоматического маппинга AniList id -> MAL id.
 *
 * GET {baseUrl}/{malId}/{episode}?types=op&types=ed&...&episodeLength={sec}
 * -> { found, results: [{ interval:{startTime,endTime}, skipType }], ... }
 * 404/500/пустой ответ трактуются как "нет сегментов", а не как ошибка UI.
 */
class AniSkipProvider(
    private val baseUrl: String = "https://api.aniskip.com/v2/skip-times",
    connectTimeoutMs: Int = 8000,
    readTimeoutMs: Int = 8000,
    private val fetch: suspend (url: String) -> String? = { url ->
        defaultFetch(url, connectTimeoutMs, readTimeoutMs)
    },
) : SkipProvider {

    override val name = "aniskip"

    override suspend fun segments(malId: Int?, episode: Int, durationMs: Long): List<SkipSegment> {
        if (malId == null || malId <= 0 || episode <= 0) return emptyList()

        val url = buildString {
            append(baseUrl).append('/').append(malId).append('/').append(episode)
            append("?types=op&types=ed&types=mixed-op&types=mixed-ed&types=recap")
            if (durationMs > 0L) {
                val seconds = durationMs / 1000.0
                append("&episodeLength=").append(seconds)
            }
        }

        val body = runCatching { fetch(url) }.getOrNull() ?: return emptyList()
        val parsed = runCatching { json.decodeFromString<AniSkipResponse>(body) }.getOrNull()
            ?: return emptyList()
        if (!parsed.found) return emptyList()

        return parsed.results
            .mapNotNull { r ->
                val type = mapType(r.skipType) ?: return@mapNotNull null
                val startMs = (r.interval.startTime * 1000.0).toLong()
                val endMs = (r.interval.endTime * 1000.0).toLong()
                if (endMs <= startMs) null else SkipSegment(type, startMs, endMs)
            }
            .sortedBy { it.startMs }
    }

    private fun mapType(raw: String): SkipType? = when (raw) {
        "op", "mixed-op" -> SkipType.OP
        "ed", "mixed-ed" -> SkipType.ED
        "recap" -> SkipType.RECAP
        else -> null
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}

/** Дефолтная реализация запроса на HttpURLConnection, без внешних HTTP-зависимостей. */
internal suspend fun defaultFetch(
    url: String,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
): String? = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("Accept", "application/json")

        val code = connection.responseCode
        if (code == 404) return@withContext null
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }
        if (code in 200..299) text else null
    } catch (_: IOException) {
        null
    } finally {
        connection.disconnect()
    }
}
