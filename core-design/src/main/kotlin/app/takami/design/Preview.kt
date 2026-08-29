package app.takami.design

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Заглушки, чтобы экраны рисовались без БД и парсеров. */
enum class Fmt(val short: String, val title: String) {
    MANGA("М", "Манга"), ANIME("А", "Аниме"), NOVEL("Р", "Ранобэ")
}

data class FakeTitle(
    val id: Int,
    val name: String,
    val fmt: Fmt,
    val source: String,
    val sub: String,
    val progress: Int,
    val badge: String? = null,
    val broken: Boolean = false,
    val cover: Brush,
)

private fun cover(a: Long, b: Long = 0xFF141821) =
    Brush.linearGradient(listOf(Color(a), Color(b)))

object Fake {
    val titles = listOf(
        FakeTitle(1, "Тайтл с длинным названием", Fmt.MANGA, "MangaHub", "Гл. 42 из 120", 35, "12", cover(0xFF3B2A6B)),
        FakeTitle(2, "Аниме сериал", Fmt.ANIME, "AniLibria", "Эп. 7 · 12:30", 60, "NEW", cover(0xFF123A4B)),
        FakeTitle(3, "Ранобэ, том 3", Fmt.NOVEL, "RanobeLib", "Гл. 5 · 43%", 43, null, cover(0xFF4B2740)),
        FakeTitle(4, "Источник недоступен", Fmt.MANGA, "AnimeGo", "Ошибка загрузки", 0, "!", cover(0xFF4A1F1F), broken = true),
        FakeTitle(5, "Ещё один тайтл", Fmt.MANGA, "MangaDex", "Гл. 1 из 88", 12, null, cover(0xFF1F4636)),
        FakeTitle(6, "Новинка сезона", Fmt.ANIME, "AnimeGo", "Завершено", 100, null, cover(0xFF4A3A16)),
    )
    val continueItem = titles.first { it.progress in 1..99 }
    fun byFmt(f: Fmt) = titles.filter { it.fmt == f }
    val reading = titles.filter { it.progress in 1..99 }
}
