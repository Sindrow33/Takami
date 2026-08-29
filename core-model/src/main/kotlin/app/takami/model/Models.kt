package app.takami.model

enum class Format { MANGA, ANIME, RANOBE }

data class SourceInfo(
    val id: String, val name: String, val lang: String,
    val lastChapter: Int, val gaps: List<Int> = emptyList()
) { fun has(n: Int) = n <= lastChapter && n !in gaps }

/** позиция хранится номером главы/эпизода, а не индексом в списке */
sealed interface Progress {
    data class Paged(val chapter: Int, val page: Int = 0) : Progress
    data class Timed(val episode: Int, val seconds: Long = 0) : Progress
    data class Textual(val chapter: Int, val percent: Float = 0f) : Progress
}

data class Title(
    val id: String, val franchiseId: String, val name: String,
    val formats: Set<Format>, val genres: List<String> = emptyList()
)
