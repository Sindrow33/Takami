package dev.takami.app.home

import android.content.Context
import dev.takami.app.data.ReadingProgressStore
import dev.takami.app.library.LibraryRoot
import dev.takami.app.library.LocalLibrary
import dev.takami.app.library.NovelLibrary
import dev.takami.app.parser.ParserState

/**
 * Наполнение главной — из того, что реально есть на устройстве.
 *
 * До этого главная целиком стояла на выдуманных данных: hero, рельсы
 * «Продолжить», «Манга», «Аниме», «Ранобэ», новости. Выглядело это как
 * работающее приложение, и дважды было принято за него — а на деле ни
 * одна карточка не открывалась во что-либо настоящее.
 *
 * Поэтому правило здесь одно: карточка появляется, только если за ней
 * стоит файл на диске или разобранный источник. Пусто — значит пусто, и
 * экран честно говорит, что делать.
 */
object LibraryFeed {

    /**
     * Карточка для главной и для любого места, где нужен тайтл.
     *
     * Форма согласована с компонентами карточек: `rating` строкой
     * (иначе «8.4» и «8,4» решаются в двух местах по-разному),
     * `coverUrl` nullable без заглушечных ссылок (карточка сама рисует
     * плейсхолдер, а фиктивный URL даёт сломанную картинку), `id` с
     * префиксом источника (иначе локальный и сетевой тайтл с одним
     * номером схлопываются).
     */
    data class TitleCardData(
        val id: String,
        val title: String,
        val kind: ContentType,
        val coverUrl: String? = null,
        val subtitle: String? = null,
        val rating: String? = null,
        val badgeCount: Int = 0,
        val progress: Float = 0f,
    )

    data class Feed(
        val continueReading: List<TitleCardData>,
        val manga: List<TitleCardData>,
        val novels: List<TitleCardData>,
        val anime: List<TitleCardData>,
        /** Выбрана ли папка с контентом — от этого зависит текст пустого экрана. */
        val folderChosen: Boolean,
        /** Есть ли разобранные источники. */
        val hasSources: Boolean,
    ) {
        val isEmpty: Boolean
            get() = continueReading.isEmpty() && manga.isEmpty() &&
                novels.isEmpty() && anime.isEmpty()

        /** Первая карточка «Продолжить» — то, что показывает hero. */
        val hero: TitleCardData? get() = continueReading.firstOrNull()
    }

    /**
     * Собирает ленту. Вызывать из фонового потока: обход выбранной папки
     * идёт через `content://`, и на большой библиотеке это заметно.
     */
    fun load(context: Context): Feed {
        val root = LibraryRoot(context)
        val tree = root.selectedTree()
        val progress = ReadingProgressStore(context)

        val mangaTitles = LocalLibrary.allTitles(context).map { it.toCard(ContentType.Manga, progress) }
        val novelTitles = tree
            ?.let { NovelLibrary.titles(context, it) }
            .orEmpty()
            .map { it.toCard(ContentType.Novel, progress) }

        /*
         * «Продолжить» — только начатое и не дочитанное. Показывать
         * здесь всю библиотеку значит превратить рельсу в дубль
         * остальных, а показывать дочитанное — предлагать перечитать
         * то, что человек только что закрыл.
         */
        val continueReading = (mangaTitles + novelTitles)
            .filter { it.progress > 0f && it.progress < 1f }
            .sortedByDescending { it.progress }

        return Feed(
            continueReading = continueReading,
            manga = mangaTitles,
            novels = novelTitles,
            // Аниме приезжает от парсера; пока разобранных источников
            // нет, рельса пуста — выдуманных серий здесь больше не будет.
            anime = emptyList(),
            folderChosen = tree != null,
            hasSources = ParserState(context).hasData,
        )
    }

    private fun LocalLibrary.Title.toCard(
        kind: ContentType,
        progress: ReadingProgressStore,
    ): TitleCardData {
        val read = chapters.count { progress.isCompleted(it.id) }
        val started = chapters.count { progress.page(it.id) > 0 || progress.charOffset(it.id) > 0 }
        return TitleCardData(
            id = "${kind.prefix}:$id",
            title = name,
            kind = kind,
            // Обложек у локальных файлов нет, и подставлять ссылку
            // некуда: карточка рисует плейсхолдер сама.
            coverUrl = null,
            subtitle = chapterLabel(kind, chapters.size),
            // Рейтинга у локального файла быть не может. Пустая строка
            // здесь была бы такой же выдумкой, как «8.7».
            rating = null,
            badgeCount = 0,
            progress = if (chapters.isEmpty()) 0f else read.toFloat() / chapters.size,
        ).let { card ->
            // Начатая, но ни одной дочитанной главы — это всё же
            // прогресс: иначе тайтл не попадёт в «Продолжить» до самой
            // первой дочитанной главы.
            if (card.progress == 0f && started > 0) card.copy(progress = MINIMAL_PROGRESS) else card
        }
    }

    private fun chapterLabel(kind: ContentType, count: Int): String {
        val noun = when (kind) {
            ContentType.Novel -> plural(count, "глава", "главы", "глав")
            else -> plural(count, "глава", "главы", "глав")
        }
        return "$count $noun"
    }

    /**
     * Русские числительные. «1 глав» и «5 глава» бросаются в глаза
     * сильнее, чем кажется при написании кода.
     */
    fun plural(count: Int, one: String, few: String, many: String): String {
        val mod100 = count % 100
        if (mod100 in 11..14) return many
        return when (count % 10) {
            1 -> one
            2, 3, 4 -> few
            else -> many
        }
    }

    private const val MINIMAL_PROGRESS = 0.01f
}

/** Префикс источника в идентификаторе карточки. */
val ContentType.prefix: String
    get() = when (this) {
        ContentType.Manga -> "manga"
        ContentType.Novel -> "novel"
        ContentType.Anime -> "anime"
    }
