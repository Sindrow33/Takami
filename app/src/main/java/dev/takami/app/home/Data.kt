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
    /** Адрес новости: карточка без него — картинка, а не новость. */
    val url: String = "",
)

enum class NewsTone(val accent: Color, val coverFrom: Color, val coverTo: Color) {
    Primary(Aurora.Acc, Color(0xFF3B2A6B), Color(0xFF141821)),
    Cyan(Aurora.Acc3, Color(0xFF123A4B), Color(0xFF141821)),
    Warn(Aurora.Warn, Color(0xFF4B2740), Color(0xFF141821)),
    Ok(Aurora.Ok, Color(0xFF1F4636), Color(0xFF141821)),
}

/**
 * Запасная лента новостей — пуста и такой останется.
 *
 * Выдуманные новости с настоящими названиями изданий дважды были
 * приняты за работающую функцию, поэтому здесь ничего нет. Настоящие
 * новости приходят от автопарсера через `DiscoveredNews` и
 * `NewsFeed.cards()`; пустой список остаётся значением по умолчанию
 * для превью и на случай, когда лента ещё не прочитана с диска.
 */
val newsFeed: List<NewsItem> = emptyList()
