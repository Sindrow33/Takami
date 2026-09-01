package dev.takami.app.home

import androidx.compose.ui.graphics.Color
import dev.takami.app.ui.theme.Aurora

enum class ContentType(val label: String, val color: Color) {
    Anime("аниме", Aurora.TypeAnime),
    Manga("манга", Aurora.TypeManga),
    Novel("ранобэ", Aurora.TypeNovel),
}

data class TitleItem(
    val id: Int,
    val name: String,
    val type: ContentType,
    val rating: String,
    val newChapters: Int = 0,
    val progress: Float = 0f,
    val subtitle: String = "",
    val coverA: Color,
    val coverB: Color,
)

/** Моки — соответствуют kit/data.js из хендоффа. В проде заменяются данными источников. */
object MockDb {
    val titles = listOf(
        TitleItem(1, "Ветер над Хакконэ", ContentType.Manga, "8.7", 12, .62f, "Глава 84 из 130", Color(0xFF4C1D95), Color(0xFF1E1B4B)),
        TitleItem(2, "Сталь и сакура", ContentType.Anime, "9.1", 3, .34f, "Эпизод 8 из 24", Color(0xFF0C4A6E), Color(0xFF0F1116)),
        TitleItem(3, "Тихий дом на холме", ContentType.Novel, "8.2", 0, .81f, "Том 3, глава 12", Color(0xFF5B21B6), Color(0xFF1E1B4B)),
        TitleItem(4, "Полночный экспресс", ContentType.Manga, "7.9", 5, .12f, "Глава 6 из 88", Color(0xFF134E4A), Color(0xFF0F1116)),
        TitleItem(5, "Клинок и облако", ContentType.Anime, "8.8", 1, .55f, "Эпизод 13 из 26", Color(0xFF7C2D12), Color(0xFF1C1917)),
        TitleItem(6, "Слова без ветра", ContentType.Novel, "8.5", 2, .27f, "Том 1, глава 30", Color(0xFF831843), Color(0xFF1E1B4B)),
    )

    val continueReading = titles.filter { it.progress > 0f }
    val hero = titles.first()

    fun byType(type: ContentType) = titles.filter { it.type == type }
}

/** Новость индустрии для карусели на главной. */
data class NewsItem(
    val id: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val source: String,
    val age: String,
    val tone: NewsTone,
)

enum class NewsTone(val accent: Color, val coverFrom: Color, val coverTo: Color) {
    Primary(Aurora.Acc, Color(0xFF3B2A6B), Color(0xFF141821)),
    Cyan(Aurora.Acc3, Color(0xFF123A4B), Color(0xFF141821)),
    Warn(Aurora.Warn, Color(0xFF4B2740), Color(0xFF141821)),
    Ok(Aurora.Ok, Color(0xFF1F4636), Color(0xFF141821)),
}

/** Лента новостей. Моки из `kit/data.js`; позже придут из источников. */
val newsFeed = listOf(
    NewsItem(
        "n1", "Индустрия", "MAPPA анонсировала финальный сезон",
        "Премьера — весна 2027, 24 эпизода одним куском",
        "Anime News Network", "2 ч назад", NewsTone.Primary,
    ),
    NewsItem(
        "n2", "Трейлер", "Первый ролик экранизации ранобэ",
        "ufotable показала 90 секунд боёвки и опенинг",
        "YouTube · ufotable", "5 ч назад", NewsTone.Cyan,
    ),
    NewsItem(
        "n3", "Манга", "Автор уходит в перерыв на 3 месяца",
        "После 12-й арки — плановая пауза, чтобы «не сгореть»",
        "Weekly Shonen Jump", "вчера", NewsTone.Warn,
    ),
    NewsItem(
        "n4", "Релиз", "На AniLibria обновлена озвучка",
        "Полная переозвучка первого сезона, +40 % битрейта",
        "AniLibria", "2 дня назад", NewsTone.Ok,
    ),
)
