package moe.scenesearch.api

sealed class SceneSearchError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class QuotaDepleted(val quota: Int, val used: Int) :
        SceneSearchError("search quota depleted (" + used + "/" + quota + ")")

    class Concurrency : SceneSearchError("provider concurrency or rate limit reached")

    class TooLarge(val bytes: Int) : SceneSearchError("image too large: " + bytes + " bytes")

    class BadImage(reason: String = "cannot decode image") : SceneSearchError(reason)

    class Unauthorized : SceneSearchError("invalid or missing api key")

    class Unavailable(reason: String, cause: Throwable? = null) : SceneSearchError(reason, cause)
}
