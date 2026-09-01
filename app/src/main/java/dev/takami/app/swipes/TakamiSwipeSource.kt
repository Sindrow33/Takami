package dev.takami.app.swipes

import android.content.Context
import dev.anime.player.host.AnimeCatalog
import dev.takami.app.library.LibraryRoot
import dev.takami.app.library.LocalLibrary
import dev.takami.swipes.DeckCard
import dev.takami.swipes.SwipeDecisionStore
import dev.takami.swipes.SwipeDirection
import dev.takami.swipes.SwipeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Карточки для подбора из реальных данных: локальная библиотека (манга и
 * аниме) плюс тайтлы, снятые автопарсером с разобранных сайтов.
 *
 * Живёт в `:app`, потому что оба источника данных здесь; модуль свайпов знает
 * только интерфейс — иначе `:feature:swipes` пришлось бы завязать на
 * приложение, а зависимость идёт в обратную сторону.
 */
class TakamiSwipeSource(private val context: Context) : SwipeSource {

    override suspend fun cards(): List<DeckCard> = withContext(Dispatchers.IO) {
        val root = LibraryRoot(context)
        val cards = mutableListOf<DeckCard>()

        // Манга из библиотеки.
        runCatching {
            LocalLibrary.titles(context).forEach { title ->
                cards += DeckCard(
                    id = "manga:" + title.id,
                    title = title.name,
                    subtitle = plural(title.chapterCount, "глава", "главы", "глав"),
                    kind = "Манга",
                )
            }
        }

        // Аниме из той же выбранной папки.
        runCatching {
            val animeTitles = root.selectedTree()
                ?.let { AnimeCatalog.scanTree(context, it) }
                ?: AnimeCatalog.scan(root.internalDir().parentFile ?: context.filesDir)
            animeTitles.forEach { title ->
                cards += DeckCard(
                    id = "anime:" + title.id,
                    title = title.name,
                    subtitle = plural(title.episodeCount, "серия", "серии", "серий"),
                    kind = "Аниме",
                )
            }
        }

        // Разобранное автопарсером — с обложками, если сайт их отдал.
        runCatching {
            DiscoveredTitles(context).all().forEach { entry ->
                cards += DeckCard(
                    id = "web:" + entry.key,
                    title = entry.title,
                    subtitle = entry.host,
                    kind = "Из сети",
                    coverUrl = entry.cover,
                    url = entry.url,
                )
            }
        }

        cards
    }

    /**
     * «Нравится» пока помечает выбор локально: полноценное добавление в
     * библиотеку требует записи тайтла в её базу, а базы у библиотеки ещё
     * нет — она читает файлы с диска. Отметку сохраняем, чтобы после
     * появления базы отложенное не пропало.
     */
    override suspend fun like(card: DeckCard) = withContext(Dispatchers.IO) {
        LikedTitles(context).add(card.id, card.title, card.url)
    }

    private fun plural(count: Int, one: String, few: String, many: String): String {
        val n = count % 100
        val last = count % 10
        val word = when {
            n in 11..14 -> many
            last == 1 -> one
            last in 2..4 -> few
            else -> many
        }
        return "$count $word"
    }
}

/** Отложенное в «нравится». Простой список, переживающий перезапуск. */
class LikedTitles(context: Context) {
    private val sp = context.getSharedPreferences("takami.swipes.liked", Context.MODE_PRIVATE)

    fun add(id: String, title: String, url: String?) {
        sp.edit().putString(id, title + "\u0000" + (url ?: "")).apply()
    }

    fun count(): Int = sp.all.size

    fun titles(): List<String> = sp.all.values
        .mapNotNull { (it as? String)?.substringBefore('\u0000') }
        .sorted()
}

/**
 * Решения по карточкам. В SharedPreferences, как и остальное состояние
 * приложения: «мимо» обязано пережить перезапуск, иначе отброшенное
 * возвращается в колоду и подбор становится каруселью.
 */
class SwipeDecisions(context: Context) : SwipeDecisionStore {
    private val sp = context.getSharedPreferences("takami.swipes.decisions", Context.MODE_PRIVATE)

    override suspend fun decidedIds(): Set<String> = sp.all.keys.toSet()

    override suspend fun record(cardId: String, direction: SwipeDirection) {
        sp.edit().putString(cardId, direction.name).apply()
    }

    override suspend fun clear() {
        sp.edit().clear().apply()
    }
}
