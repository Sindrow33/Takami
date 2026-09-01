package com.mangareader.reader.engine.layout

/**
 * Навигация в постраничных режимах — чистая арифметика над индексами.
 *
 * Написано без `View`, `Canvas` и `MotionEvent` намеренно: в JVM-тестах
 * они заглушки, падающие с «Stub!», и логика поверх них непроверяема, а
 * непроверяемая логика в этом проекте уже трижды оказывалась написанной
 * и ни разу не вызванной.
 *
 * Что здесь решается и почему это не тривиально:
 *
 * 1. **Направление.** В RTL «следующая страница» — это индекс+1 в
 *    ленте, но визуально она приходит слева, и жест смахивания в ту же
 *    сторону обязан означать то же самое, что тап по боковой зоне.
 *    Знак смещения и знак жеста разъезжаются легко, и тогда «вперёд»
 *    на одном экране означает две разные вещи.
 *
 * 2. **Границы главы.** Дойдя до последней страницы, читатель должен
 *    попасть в следующую главу, а не упереться. При этом лента уже
 *    содержит страницы соседних глав вперемешку — прыгать «на главу»
 *    нельзя, надо просто идти по ленте и замечать смену `chapterId`.
 *
 * 3. **Конец ленты.** За последней страницей стоит `EndCap`. Он не
 *    страница, но и пропускать его нельзя: это и есть экран «вы всё
 *    прочитали».
 */
object PagedNavigator {

    /**
     * Индексы элементов, на которых постраничный режим может стоять:
     * страницы и торцевые заглушки, в порядке ленты.
     */
    fun stops(items: List<FeedItem>): List<Int> =
        items.indices.filter { items[it] is FeedItem.Page || items[it] is FeedItem.EndCap }

    /**
     * Куда переходит листание из позиции [currentIndex].
     *
     * @param forward «вперёд» в смысле чтения, а не экрана: в RTL это
     *   визуально влево. Зеркалит вызывающая сторона — здесь порядок
     *   всегда порядок ленты, чтобы направление не переворачивалось
     *   дважды.
     * @return индекс в [items], или null если дальше идти некуда.
     */
    fun step(items: List<FeedItem>, currentIndex: Int, forward: Boolean): Int? {
        val stops = stops(items)
        if (stops.isEmpty()) return null
        val position = stops.indexOf(currentIndex)
        // Позиция вне списка остановок (например, ещё не устоявшийся
        // вьюпорт) — идём к ближайшей допустимой, а не отказываем.
        if (position < 0) return stops.minByOrNull { kotlin.math.abs(it - currentIndex) }
        val next = position + if (forward) 1 else -1
        return stops.getOrNull(next)
    }

    /**
     * Номер страницы внутри её главы и общее число страниц главы.
     *
     * Считается по ленте, а не по источнику: лента — единственное
     * место, где известно, что уже подгружено, и подпись «3 / 20»
     * обязана совпадать с тем, по чему реально листают.
     */
    fun pageInChapter(items: List<FeedItem>, index: Int): Pair<Int, Int>? {
        val page = items.getOrNull(index) as? FeedItem.Page ?: return null
        val inChapter = items.filterIsInstance<FeedItem.Page>()
            .filter { it.chapterId == page.chapterId }
        val position = inChapter.indexOfFirst { it.pageRef.id == page.pageRef.id }
        if (position < 0) return null
        return position to inChapter.size
    }

    /**
     * Пересёк ли переход [fromIndex] → [toIndex] границу главы.
     *
     * Нужно для тихой смены заголовка и короткого отклика — в этом
     * проекте граница глав намеренно не рисуется отдельным экраном.
     */
    fun crossesChapter(items: List<FeedItem>, fromIndex: Int, toIndex: Int): Boolean {
        val from = items.getOrNull(fromIndex) as? FeedItem.Page ?: return false
        val to = items.getOrNull(toIndex) as? FeedItem.Page ?: return false
        return from.chapterId != to.chapterId
    }

    /**
     * Индекс элемента ленты для страницы [pageIndex] главы [chapterId].
     * Нужен слайдеру и восстановлению прогресса.
     */
    fun indexOfPage(items: List<FeedItem>, chapterId: String, pageIndex: Int): Int? {
        var seen = 0
        items.forEachIndexed { index, item ->
            if (item is FeedItem.Page && item.chapterId == chapterId) {
                if (seen == pageIndex) return index
                seen++
            }
        }
        return null
    }

    /**
     * Порог смахивания: доля ширины экрана, после которой палец
     * считается перелистнувшим.
     *
     * Отдельно от скорости намеренно. Медленное, но длинное движение —
     * это осознанный перелист; быстрый короткий рывок тоже перелист.
     * Порог по одному расстоянию заставлял бы дотягивать палец через
     * пол-экрана, порог по одной скорости — ловил бы случайные касания.
     */
    fun shouldFlip(dragFraction: Float, velocityPxPerSec: Float, viewportWidthPx: Int): Boolean {
        if (viewportWidthPx <= 0) return false
        val fastEnough = kotlin.math.abs(velocityPxPerSec) > FLING_VELOCITY_PX_S
        val farEnough = kotlin.math.abs(dragFraction) > FLIP_FRACTION
        // Быстрый рывок засчитывается только если он вообще сдвинул
        // страницу: иначе тап с дрожанием пальца листал бы.
        return farEnough || (fastEnough && kotlin.math.abs(dragFraction) > MIN_FLING_FRACTION)
    }

    const val FLIP_FRACTION = 0.28f
    const val MIN_FLING_FRACTION = 0.05f
    const val FLING_VELOCITY_PX_S = 900f
}
