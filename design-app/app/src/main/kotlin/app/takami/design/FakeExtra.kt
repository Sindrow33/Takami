package app.takami.design

/** Источник тайтла: имя, язык, максимальная глава, живой ли. */
data class FakeSource(
    val id: String,
    val name: String,
    val lang: String,
    val maxChapter: Int,
    val broken: Boolean = false,
)

data class FakeChapter(
    val number: Int,
    val name: String,
    val date: String,
    val read: Boolean,
    val available: Boolean,
)

object FakeSources {
    val pool = listOf(
        FakeSource("remanga", "ReManga", "RU", 124),
        FakeSource("mangalib", "MangaLib", "RU", 118),
        FakeSource("mangadex", "MangaDex", "EN", 98),
        FakeSource("desu", "Desu", "RU", 76),
    )

    /** Правило из прототипа: берём живой источник с наибольшей последней главой. */
    fun pick(broken: Set<String>): FakeSource? =
        pool.filter { it.id !in broken }.maxByOrNull { it.maxChapter }

    fun chapters(src: FakeSource, progress: Int): List<FakeChapter> =
        (src.maxChapter downTo 1).take(24).map { n ->
            FakeChapter(
                number = n,
                name = "Глава $n",
                date = "${(n % 28) + 1}.05.2026",
                read = n <= progress,
                available = n % 17 != 0,
            )
        }
}
