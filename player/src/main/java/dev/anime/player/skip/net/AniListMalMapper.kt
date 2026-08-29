package dev.anime.player.skip.net

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Резолвит AniList id -> MAL id через публичный GraphQL API (без ключа, но с рейт-лимитом). */
interface AniListMalMapper {
    suspend fun malIdFor(aniListId: Int): Int?
}

@Serializable
private data class GraphQlRequest(val query: String, val variables: Map<String, Int>)

@Serializable
private data class MediaDto(val id: Int, val idMal: Int? = null)

@Serializable
private data class MediaDataDto(val Media: MediaDto? = null)

@Serializable
private data class GraphQlResponseDto(val data: MediaDataDto? = null)

/**
 * Простой in-memory кэш поверх AniList GraphQL (https://docs.anilist.co).
 * Не персистентный — на процесс жизни приложения этого достаточно, т.к.
 * список эпизодов запрашивается один раз при открытии тайтла.
 */
class CachingAniListMalMapper(
    private val endpoint: String = "https://graphql.anilist.co",
    connectTimeoutMs: Int = 8000,
    readTimeoutMs: Int = 8000,
    private val post: suspend (String) -> String? = { body ->
        defaultPost(endpoint, body, connectTimeoutMs, readTimeoutMs)
    },
) : AniListMalMapper {

    private val cache = HashMap<Int, Int?>()
    private val mutex = Mutex()

    override suspend fun malIdFor(aniListId: Int): Int? {
        mutex.withLock { if (cache.containsKey(aniListId)) return cache[aniListId] }

        val bodyJson = json.encodeToString(
            GraphQlRequest.serializer(),
            GraphQlRequest(QUERY, mapOf("id" to aniListId)),
        )
        val raw = runCatching { post(bodyJson) }.getOrNull()
        val malId = raw
            ?.let { runCatching { json.decodeFromString<GraphQlResponseDto>(it) }.getOrNull() }
            ?.data?.Media?.idMal

        mutex.withLock { cache[aniListId] = malId }
        return malId
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private const val QUERY =
            "query(\$id:Int){Media(id:\$id,type:ANIME){id idMal}}"
    }
}

internal suspend fun defaultPost(
    endpoint: String,
    jsonBody: String,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
): String? = withContext(Dispatchers.IO) {
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }
        if (code in 200..299) text else null
    } catch (_: IOException) {
        null
    } finally {
        connection.disconnect()
    }
}
