package moe.scenesearch.core.tracemoe

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class TraceMoeResponse(
    val frameCount: Int = 0,
    val error: String = "",
    val result: List<TraceMoeResult> = emptyList(),
)

@Serializable
internal data class TraceMoeResult(
    val anilist: TraceMoeAnilist? = null,
    val filename: String = "",
    val episode: JsonElement? = null,
    val from: Double = 0.0,
    val to: Double = 0.0,
    val similarity: Double = 0.0,
    val video: String? = null,
    val image: String? = null,
)

@Serializable
internal data class TraceMoeAnilist(
    val id: Int = 0,
    val idMal: Int? = null,
    val title: TraceMoeTitle = TraceMoeTitle(),
    val synonyms: List<String> = emptyList(),
    val isAdult: Boolean = false,
)

@Serializable
internal data class TraceMoeTitle(
    val native: String? = null,
    val romaji: String? = null,
    val english: String? = null,
)

@Serializable
internal data class TraceMoeMe(
    val id: String = "",
    val priority: Int = 0,
    val concurrency: Int = 1,
    val quota: Int = 0,
    val quotaUsed: Int = 0,
)
