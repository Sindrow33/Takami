package dev.takami.swipes

/**
 * Логика колоды свайпов: чистые функции над числами и списком, без Compose и
 * Android-типов — то, что реально проверяется JVM-тестом.
 *
 * Карточка уходит вправо («хочу смотреть»), влево («не интересно») или
 * возвращается на место, если тянули недостаточно далеко.
 */
enum class SwipeDirection { Like, Skip, None }

data class DeckCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: String,
    /** Обложка, если источник её отдал. */
    val coverUrl: String? = null,
    /** Ссылка на тайтл — по ней «нравится» попадает в библиотеку. */
    val url: String? = null,
)

data class DeckState(
    val cards: List<DeckCard>,
    val index: Int = 0,
    val liked: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
) {
    val current: DeckCard? get() = cards.getOrNull(index)
    val next: DeckCard? get() = cards.getOrNull(index + 1)
    val isFinished: Boolean get() = index >= cards.size
    val remaining: Int get() = (cards.size - index).coerceAtLeast(0)

    fun apply(direction: SwipeDirection): DeckState {
        val card = current ?: return this
        return when (direction) {
            SwipeDirection.None -> this
            SwipeDirection.Like -> copy(index = index + 1, liked = liked + card.id)
            SwipeDirection.Skip -> copy(index = index + 1, skipped = skipped + card.id)
        }
    }

    fun restart(): DeckState = copy(index = 0, liked = emptyList(), skipped = emptyList())
}

object SwipeMath {

    /** Доля ширины экрана, после которой карточка считается свайпнутой. */
    const val COMMIT_FRACTION = 0.28f

    /** Максимальный наклон карточки в градусах на полном отклонении. */
    const val MAX_TILT_DEG = 12f

    fun directionFor(dragX: Float, widthPx: Float): SwipeDirection {
        if (widthPx <= 0f) return SwipeDirection.None
        val threshold = widthPx * COMMIT_FRACTION
        return when {
            dragX >= threshold -> SwipeDirection.Like
            dragX <= -threshold -> SwipeDirection.Skip
            else -> SwipeDirection.None
        }
    }

    /** -1f..1f: насколько карточка утянута, для наклона и яркости подсказки. */
    fun dragProgress(dragX: Float, widthPx: Float): Float {
        if (widthPx <= 0f) return 0f
        return (dragX / (widthPx * COMMIT_FRACTION)).coerceIn(-1f, 1f)
    }

    fun tiltDegrees(dragX: Float, widthPx: Float): Float =
        dragProgress(dragX, widthPx) * MAX_TILT_DEG

    /** Прозрачность метки «Хочу» / «Мимо» — появляется на половине пути до порога. */
    fun badgeAlpha(dragX: Float, widthPx: Float): Float =
        (kotlin.math.abs(dragProgress(dragX, widthPx)) * 2f - 0.15f).coerceIn(0f, 1f)

    /**
     * Масштаб карточки, лежащей под текущей: подрастает по мере утягивания верхней,
     * поэтому колода выглядит живой, а не статичной картинкой.
     */
    fun underCardScale(dragX: Float, widthPx: Float): Float =
        0.94f + 0.06f * kotlin.math.abs(dragProgress(dragX, widthPx))

    /** Куда доводить карточку при вылете за экран. */
    fun flyAwayX(direction: SwipeDirection, widthPx: Float): Float = when (direction) {
        SwipeDirection.Like -> widthPx * 1.4f
        SwipeDirection.Skip -> -widthPx * 1.4f
        SwipeDirection.None -> 0f
    }

    /**
     * Колода из уже принятых решений: карточки, по которым свайп не делали.
     *
     * Отброшенные не возвращаются — «мимо» должно означать «мимо», иначе
     * подбор превращается в бесконечную карусель одного и того же. Отложенные
     * в «нравится» тоже убираем: они уже в библиотеке.
     */
    fun filterUndecided(
        cards: List<DeckCard>,
        decided: Set<String>,
    ): List<DeckCard> = cards.filterNot { it.id in decided }
}
