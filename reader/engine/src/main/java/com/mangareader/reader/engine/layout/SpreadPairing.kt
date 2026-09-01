package com.mangareader.reader.engine.layout

/**
 * Разбиение страниц на развороты для альбомной ориентации.
 *
 * Чистая арифметика над числами: на вход — какие элементы ленты
 * являются страницами и какие из них широкие, на выход — что показать
 * на каждом экране. Ни `Bitmap`, ни `View` здесь нет намеренно, иначе
 * это было бы непроверяемо в JVM.
 *
 * Три правила, каждое из которых ломает наивное «клеим по две»:
 *
 * 1. **Широкая страница занимает экран целиком.** В манге разворот
 *    художника — это один кадр на два листа, отсканированный одной
 *    картинкой. Поставленный в пару, он ужимается вдвое и стоит рядом
 *    с обычной страницей — то есть ровно тот кадр, ради которого
 *    разворот и включают, выглядит хуже, чем без разворота.
 *
 * 2. **Широкая страница сдвигает чётность.** После неё пары начинаются
 *    заново, иначе весь остаток главы склеен со сдвигом на страницу, и
 *    развороты художника перестают совпадать с листами.
 *
 * 3. **Границу главы пара не пересекает.** Последняя страница главы и
 *    первая следующей — не разворот, это две разные книги на одном
 *    экране.
 */
object SpreadPairing {

    /**
     * Что показывается на одном экране.
     *
     * @param first индекс в исходном списке элементов ленты.
     * @param second второй индекс пары, или null если страница одна
     *   (широкая, последняя в главе, или торец ленты).
     */
    data class Spread(val first: Int, val second: Int?)

    /**
     * Признаки одной позиции ленты, нужные для разбиения.
     *
     * @param isPage торцы ленты страницами не являются и в пары не
     *   попадают, но своим экраном остаются.
     * @param isWide ширина больше высоты — разворот художника.
     * @param chapterId по нему проходит запрет на склейку через границу.
     */
    data class Slot(val isPage: Boolean, val isWide: Boolean, val chapterId: String?)

    /**
     * @param rtl порядок внутри пары. В RTL правая половина читается
     *   первой — значит `first` рисуется справа. Сам порядок списка не
     *   переворачивается: переворот и здесь, и в навигации дал бы
     *   двойную инверсию, то есть возврат к исходному.
     */
    fun pair(slots: List<Slot>, rtl: Boolean = false): List<Spread> {
        val result = ArrayList<Spread>()
        var index = 0
        while (index < slots.size) {
            val slot = slots[index]
            if (!slot.isPage || slot.isWide) {
                result += Spread(index, null)
                index++
                continue
            }
            val nextIndex = index + 1
            val next = slots.getOrNull(nextIndex)
            val pairable = next != null &&
                next.isPage &&
                !next.isWide &&
                next.chapterId == slot.chapterId
            if (pairable) {
                result += Spread(index, nextIndex)
                index += 2
            } else {
                result += Spread(index, null)
                index++
            }
        }
        return result
    }

    /** Номер разворота, на котором лежит элемент [itemIndex]. */
    fun spreadOf(spreads: List<Spread>, itemIndex: Int): Int? {
        val position = spreads.indexOfFirst { it.first == itemIndex || it.second == itemIndex }
        return if (position >= 0) position else null
    }

    /**
     * Разворот применим только в альбомной ориентации.
     *
     * В портрете две страницы рядом дают полосу вдвое уже экрана —
     * текст в ней нечитаем. Поэтому настройка включает разворот, а
     * ориентация решает, действует ли он сейчас; для пользователя это
     * выглядит как «повернул телефон — стало два листа».
     */
    fun applies(enabled: Boolean, viewportWidthPx: Int, viewportHeightPx: Int): Boolean =
        enabled && viewportWidthPx > viewportHeightPx
}
