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

        val top = countUniform(rowMeans, tolerance, limit, fromStart = true)
        val bottom = countUniform(rowMeans, tolerance, limit, fromStart = false)

        // Если «поле» съело всю страницу, значит страница просто
        // однотонная (разделитель, пустой лист) — резать нечего.
        if (top + bottom >= rowMeans.size) return Crop(0, 0)
        return Crop(top, bottom)
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
