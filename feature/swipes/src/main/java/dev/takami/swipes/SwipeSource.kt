package dev.takami.swipes

/**
 * Откуда берутся карточки. Интерфейс, а не прямая зависимость от модуля
 * приложения: локальная библиотека и автопарсер живут в `:app`, а `:app`
 * зависит от `:feature:swipes`, не наоборот.
 *
 * Хост реализует это поверх своих данных и передаёт в [SwipesScreen].
 */
interface SwipeSource {
    /** Карточки для подбора; пустой список — нормальное состояние, не ошибка. */
    suspend fun cards(): List<DeckCard>

    /** Отложить тайтл в библиотеку. */
    suspend fun like(card: DeckCard)
}

/**
 * Решения пользователя: что уже показывали и куда свайпнули.
 *
 * Отдельно от источника, потому что живёт дольше: список карточек меняется
 * с каждым разбором сайта, а «мимо» по конкретному тайтлу должно
 * сохраниться между запусками — иначе отброшенное возвращается.
 */
interface SwipeDecisionStore {
    suspend fun decidedIds(): Set<String>
    suspend fun record(cardId: String, direction: SwipeDirection)
    suspend fun clear()
}

/** Пустая реализация — для превью и для случая, когда хост ничего не передал. */
object NoSwipeSource : SwipeSource {
    override suspend fun cards(): List<DeckCard> = emptyList()
    override suspend fun like(card: DeckCard) = Unit
}

object NoDecisionStore : SwipeDecisionStore {
    private val decided = mutableSetOf<String>()
    override suspend fun decidedIds(): Set<String> = decided.toSet()
    override suspend fun record(cardId: String, direction: SwipeDirection) {
        decided.add(cardId)
    }

    override suspend fun clear() {
        decided.clear()
    }
}
