package app.takami.core

enum class Format { MANGA, ANIME, RANOBE }

data class SourceInfo(
    val id: String,
    val name: String,
    val lang: String = "ru",
    val lastChapter: Int,
    val gaps: Set<Int> = emptySet(),
    val latencyMs: Int = 0
) {
    fun has(n: Int) = n in 1..lastChapter && n !in gaps
    fun chapters(): List<Int> = (1..lastChapter).filter { it !in gaps }
}

/** позиция всегда номер главы/эпизода, никогда не индекс в списке */
sealed interface Progress {
    val chapter: Int
    data class Paged(override val chapter: Int, val page: Int = 0) : Progress
    data class Timed(override val chapter: Int, val seconds: Long = 0) : Progress
    data class Textual(override val chapter: Int, val percent: Float = 0f) : Progress
}
