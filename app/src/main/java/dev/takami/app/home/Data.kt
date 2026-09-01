package dev.takami.app.home

import androidx.compose.ui.graphics.Color
import dev.takami.app.ui.theme.Aurora

enum class ContentType(val label: String, val color: Color) {
    Anime("аниме", Aurora.TypeAnime),
    Manga("манга", Aurora.TypeManga),
    Novel("ранобэ", Aurora.TypeNovel),
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

/**
 * Лента новостей.
 *
 * Пуста, и это не заготовка: выдуманные новости с настоящими
 * названиями изданий выглядели как работающая функция и дважды были
 * приняты за неё. Настоящие приедут от автопарсера новостных сайтов —
 * до тех пор карусель просто не показывается.
 *
 * Тип и оформление карточки остаются: подставить сюда разобранные
 * новости — это одна строка, а не новый экран.
 */
val newsFeed: List<NewsItem> = emptyList()
