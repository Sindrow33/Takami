package moe.scenesearch.api

/** Внешний движок идентификации сцены: trace.moe, SauceNAO, свой индекс. */
interface SceneSearchProvider {
    val id: String
    val supports: Set<MediaKind>

    suspend fun identify(query: ImageQuery): List<SceneIdentity>

    suspend fun quota(): QuotaInfo? = null
}

data class SceneSearchOutcome(
    val results: List<SceneIdentity> = emptyList(),
    val fromCache: Boolean = false,
    val providerId: String? = null,
    val errors: List<SceneSearchError> = emptyList(),
) {
    val best: SceneIdentity? get() = results.maxByOrNull { it.similarity }
    val isEmpty: Boolean get() = results.isEmpty()
}
