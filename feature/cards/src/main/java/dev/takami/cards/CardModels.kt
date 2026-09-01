package dev.takami.cards

/**
 * Данные карточки тайтла.
 *
 * Модель компонента, а не источника: карточка должна рисоваться одинаково,
 * пришёл тайтл из локальной папки или с разобранного сайта. Источник
 * приводит своё к этой форме одним маппингом.
 */
data class TitleCardData(
    /**
     * Идентификатор с префиксом источника (`manga:`, `anime:`, `web:`).
     * Без префикса локальный и сетевой тайтл с одинаковым номером
     * схлопываются в один — уже ловил это в свайпах.
     */
    val id: String,
    val title: String,
    val kind: ContentKind,
    /**
     * Обложка. `null` — обычный случай, а не ошибка: автопарсер берёт
     * обложку, когда она есть в разметке, и это не гарантия.
     */
    val coverUrl: String? = null,
    val subtitle: String? = null,
    /** Готовая строка, а не число: иначе формат «8.4»/«8,4» решается в двух местах по-разному. */
    val rating: String? = null,
    /** Непрочитанное; 0 — бейджа нет. */
    val badgeCount: Int = 0,
    /** Прогресс 0f..1f; 0f — полоски нет. */
    val progress: Float = 0f,
)

enum class ContentKind { Anime, Manga, Novel }

/**
 * Данные карточки персонажа.
 *
 * Источника данных по персонажам в проекте нет вообще, поэтому модель
 * намеренно узкая: ровно то, что показывает макет. Расширять её под
 * несуществующие данные значило бы проектировать вслепую.
 */
data class CharacterCardData(
    val id: String,
    val name: String,
    /** Имя на языке оригинала, если источник его отдал. */
    val nativeName: String? = null,
    val imageUrl: String? = null,
    /** Роль: главная, второстепенная. */
    val role: CharacterRole = CharacterRole.Unknown,
    /** Актёр озвучки. */
    val voiceActor: String? = null,
)

enum class CharacterRole { Main, Supporting, Unknown }

object CardText {

    fun kindLabel(kind: ContentKind): String = when (kind) {
        ContentKind.Anime -> "Аниме"
        ContentKind.Manga -> "Манга"
        ContentKind.Novel -> "Ранобэ"
    }

    fun roleLabel(role: CharacterRole): String? = when (role) {
        CharacterRole.Main -> "Главный"
        CharacterRole.Supporting -> "Второстепенный"
        CharacterRole.Unknown -> null
    }

    /**
     * Бейдж непрочитанного. Двузначное число не влезает в жёсткий круг,
     * поэтому форма «99+» — ту же ошибку уже ловили на календаре.
     */
    fun badgeLabel(count: Int): String? = when {
        count <= 0 -> null
        count > 99 -> "99+"
        else -> count.toString()
    }

    /**
     * Инициалы для плейсхолдера обложки: карточка без картинки должна
     * оставаться узнаваемой, а не быть пустым прямоугольником.
     */
    fun initials(text: String): String {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }

    /**
     * Устойчивый цвет плейсхолдера по идентификатору: один и тот же тайтл
     * всегда одного цвета, иначе список «мигает» при каждой перерисовке.
     */
    fun placeholderIndex(id: String, paletteSize: Int): Int {
        if (paletteSize <= 0) return 0
        var hash = 0
        for (ch in id) hash = hash * 31 + ch.code
        return ((hash % paletteSize) + paletteSize) % paletteSize
    }

    /** Прогресс: 0f..1f, всё остальное — не показывать. */
    fun showProgress(progress: Float): Boolean = progress > 0.001f

    fun clampProgress(progress: Float): Float = progress.coerceIn(0f, 1f)
}
