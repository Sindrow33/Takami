package com.mangareader.reader.engine.decode

/**
 * Поиск однотонных полей страницы для режима «обрезать поля».
 *
 * Вынесено чистой функцией над яркостями строк/столбцов, а не над
 * `Bitmap`: обрезка — это решение «сколько пикселей отрезать», и его
 * можно и нужно проверять в JVM-тестах. Работа с самим битмапом
 * остаётся в вызывающем коде.
 *
 * Сканы часто имеют белые (или чёрные, если инвертированы) поля разной
 * ширины сверху и снизу. Обрезка по фиксированному проценту тут не
 * годится: у одной страницы поля 2%, у соседней 15%, и лента поедет.
 */
object BorderCropper {

    /** Максимальная доля страницы, которую разрешено срезать с одной стороны. */
    const val MAX_CROP_FRACTION = 0.25f

    /**
     * Насколько поле обязано отличаться от содержимого страницы, чтобы
     * считаться полем.
     *
     * Без этой проверки любой плавный градиент у края читается как
     * поле: соседние строки отличаются на единицы, накапливается
     * длинный «однотонный» участок, и обрезка съедает живой рисунок.
     * Поле — это не просто ровный участок, а ровный участок,
     * контрастный к странице.
     */
    const val MIN_FIELD_CONTRAST = 24f

    data class Crop(val top: Int, val bottom: Int) {
        val isEmpty: Boolean get() = top == 0 && bottom == 0
    }

    /**
     * @param rowMeans средняя яркость каждой строки пикселей, 0..255.
     * @param tolerance допустимое отклонение от цвета поля.
     * @return сколько строк отрезать сверху и снизу.
     */
    fun detect(rowMeans: FloatArray, tolerance: Float = 6f): Crop {
        if (rowMeans.size < 4) return Crop(0, 0)

        val limit = (rowMeans.size * MAX_CROP_FRACTION).toInt()
        if (limit <= 0) return Crop(0, 0)

        // Опорная яркость содержимого — медиана центральной трети.
        // Медиана, а не среднее: одна тёмная плашка не должна утащить
        // за собой оценку всей страницы.
        val content = contentLevel(rowMeans)

        val top = if (isField(rowMeans.first(), content)) {
            countUniform(rowMeans, tolerance, limit, fromStart = true)
        } else {
            0
        }
        val bottom = if (isField(rowMeans.last(), content)) {
            countUniform(rowMeans, tolerance, limit, fromStart = false)
        } else {
            0
        }

        // Если «поле» съело всю страницу, значит страница просто
        // однотонная (разделитель, пустой лист) — резать нечего.
        if (top + bottom >= rowMeans.size) return Crop(0, 0)
        return Crop(top, bottom)
    }

    private fun isField(edgeValue: Float, contentLevel: Float): Boolean =
        kotlin.math.abs(edgeValue - contentLevel) >= MIN_FIELD_CONTRAST

    private fun contentLevel(values: FloatArray): Float {
        val from = values.size / 3
        val to = values.size - values.size / 3
        val middle = values.copyOfRange(from, to.coerceAtLeast(from + 1)).sortedArray()
        return middle[middle.size / 2]
    }

    private fun countUniform(
        values: FloatArray,
        tolerance: Float,
        limit: Int,
        fromStart: Boolean,
    ): Int {
        val edgeValue = if (fromStart) values.first() else values.last()
        var count = 0
        while (count < limit) {
            val index = if (fromStart) count else values.lastIndex - count
            if (kotlin.math.abs(values[index] - edgeValue) > tolerance) break
            count++
        }
        // Одна-две строки — это не поле, а край скана; не трогаем,
        // иначе обрезка даёт дрожание на пиксель между страницами.
        return if (count <= 2) 0 else count
    }
}
