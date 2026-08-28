package moe.scenesearch.api

/**
 * Мост к твоим парсерам. Модуль поиска не знает про HTML и сайты,
 * он видит только этот контракт, который реализует слой источников.
 */
data class ExternalIds(
    val anilist: Int? = null,
    val mal: Int? = null,
    val shikimori: Int? = null,
    val kitsu: Int? = null,
) {
    val isEmpty: Boolean get() = anilist == null && mal == null && shikimori == null && kitsu == null
}

data class SourceEpisode(
    val number: Double,
    val url: String,
    val title: String? = null,
)

data class SourceEntry(
    val sourceId: String,
    val id: String,
    val title: String,
    val url: String,
    val altTitles: List<String> = emptyList(),
    val externalIds: ExternalIds = ExternalIds(),
    /** Сколько серий шло до этого сезона, если источник нумерует внутри сезона. */
    val absoluteOffset: Int? = null,
    val episodes: List<SourceEpisode> = emptyList(),
)

interface ContentSource {
    val id: String
    val name: String
    val kind: MediaKind

    suspend fun search(query: String): List<SourceEntry>

    suspend fun episodes(entry: SourceEntry): List<SourceEpisode> = entry.episodes

    /** Реализуй, если сайт отдаёт shikimori или mal id: тогда матч будет точным. */
    suspend fun findByExternalId(ids: ExternalIds): SourceEntry? = null
}

interface SourceRegistry {
    fun enabled(kind: MediaKind): List<ContentSource>
}

data class EpisodeMatch(
    val episode: SourceEpisode,
    val startAtSec: Double,
)

data class ResolvedScene(
    val entry: SourceEntry,
    val episode: EpisodeMatch?,
    val confidence: Double,
    val identity: SceneIdentity,
) {
    val sourceId: String get() = entry.sourceId
}
