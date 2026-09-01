package com.mangareader.reader.engine.novel

/**
 * Позиция чтения в текстовой главе — в символах, а не в пикселях.
 *
 * Причина, по которой это отдельный тип, а не число прокрутки: у текста
 * нет фиксированной высоты. Пользователь меняет размер шрифта,
 * межстрочный интервал, поля, поворачивает экран — и та же самая глава
 * становится вдвое длиннее или короче. Сохранённая позиция в пикселях
 * после этого указывает в произвольное место, причём тем дальше от
 * нужного, чем дальше человек прочитал.
 *
 * Символ же остаётся тем же символом при любой разметке. Поэтому
 * прогресс хранится как смещение в символах от начала главы, а перевод
 * в экраны и обратно делает разметка — она одна знает, что и куда
 * поместилось при текущих настройках.
 *
 * Доля прочитанного здесь тоже считается по символам, а не по абзацам:
 * абзацы бывают в одну строку и на пол-экрана, и «5 из 20 абзацев»
 * ничего не говорит о том, сколько осталось читать.
 */
@JvmInline
value class NovelPosition(val charOffset: Int) {

    fun coerceIn(totalChars: Int): NovelPosition =
        NovelPosition(charOffset.coerceIn(0, maxOf(0, totalChars)))

    companion object {
        val START = NovelPosition(0)

        /**
         * Доля прочитанного, 0..1.
         *
         * Пустая глава — это 0, а не деление на ноль и не «прочитано
         * полностью»: показывать 100% на пустом тексте значит соврать.
         */
        fun progress(offset: Int, totalChars: Int): Float {
            if (totalChars <= 0) return 0f
            return (offset.toFloat() / totalChars).coerceIn(0f, 1f)
        }

        /** Обратный перевод: доля → смещение. Нужен слайдеру. */
        fun offsetOf(progress: Float, totalChars: Int): Int =
            (progress.coerceIn(0f, 1f) * totalChars).toInt().coerceIn(0, maxOf(0, totalChars))
    }
}

/**
 * Глава ранобэ в том виде, в котором её показывает читалка.
 *
 * Абзацы приходят от парсера или из локального файла уже разобранными;
 * здесь к ним добавляется единственное, что нужно позиции, — смещение
 * начала каждого абзаца в символах.
 */
data class NovelChapter(
    val id: String,
    val title: String?,
    val paragraphs: List<String>,
) {
    /**
     * Смещение начала каждого абзаца, в символах. Между абзацами
     * считается один разделитель — иначе смещения, посчитанные здесь и
     * в разметке, разъедутся, и позиция будет уезжать тем сильнее, чем
     * больше абзацев прочитано.
     */
    val paragraphOffsets: List<Int> by lazy {
        var running = 0
        paragraphs.map { paragraph ->
            val start = running
            running += paragraph.length + PARAGRAPH_SEPARATOR_LENGTH
            start
        }
    }

    val totalChars: Int by lazy {
        if (paragraphs.isEmpty()) 0
        else paragraphOffsets.last() + paragraphs.last().length
    }

    /** Абзац, внутри которого лежит [offset]. */
    fun paragraphAt(offset: Int): Int {
        if (paragraphs.isEmpty()) return 0
        val index = paragraphOffsets.indexOfLast { it <= offset }
        return if (index < 0) 0 else index
    }

    companion object {
        const val PARAGRAPH_SEPARATOR_LENGTH = 1
    }
}
