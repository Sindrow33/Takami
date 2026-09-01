package com.mangareader.reader.engine.novel

/**
 * Разбиение главы на экраны для постраничного режима.
 *
 * Чистая арифметика над числами: на вход — длины абзацев и сколько
 * символов помещается на экран, на выход — границы экранов в символах.
 * Ни `TextPaint`, ни `StaticLayout` здесь нет намеренно — в JVM это
 * заглушки, а разметка, которую нельзя проверить тестом, ошибается
 * молча: пропущенная строка на стыке экранов выглядит как опечатка
 * автора, а не как дефект.
 *
 * Точность здесь важнее, чем кажется. Экран, посчитанный на символ
 * длиннее, съедает первую строку следующего; на символ короче —
 * повторяет последнюю. И то и другое читатель замечает не сразу, а
 * через несколько страниц, когда текст перестаёт сходиться.
 */
object NovelPaginator {

    /**
     * Границы экранов: смещение начала каждого экрана в символах.
     * Первый элемент всегда 0, даже у пустой главы — экран есть всегда,
     * пусть и пустой.
     */
    fun paginate(chapter: NovelChapter, charsPerScreen: Int): List<Int> {
        if (charsPerScreen <= 0 || chapter.paragraphs.isEmpty()) return listOf(0)

        val starts = ArrayList<Int>()
        starts += 0
        var screenStart = 0
        var used = 0

        chapter.paragraphs.forEachIndexed { index, paragraph ->
            val paragraphStart = chapter.paragraphOffsets[index]
            var consumed = 0
            while (consumed < paragraph.length) {
                val room = charsPerScreen - used
                if (room <= 0) {
                    screenStart = paragraphStart + consumed
                    starts += screenStart
                    used = 0
                    continue
                }
                val take = minOf(room, paragraph.length - consumed)
                consumed += take
                used += take
            }
            /*
             * Абзац кончился — остаток строки на экране не
             * переиспользуется. Следующий абзац начинается с новой
             * строки, и считать, что он продолжит недописанную, значит
             * систематически недооценивать длину: на главе из коротких
             * реплик ошибка накапливается в целые экраны.
             */
            used = minOf(charsPerScreen, used + estimateLineTail(paragraph, charsPerScreen))
        }
        return starts
    }

    /**
     * Сколько места «пропадает» в конце абзаца — хвост незаполненной
     * последней строки. Оценка, а не точный расчёт: точный требует
     * ширины символов, то есть шрифта, то есть Android.
     */
    private fun estimateLineTail(paragraph: String, charsPerScreen: Int): Int {
        if (paragraph.isEmpty()) return 0
        // Строк на экране примерно столько, сколько символов на экран,
        // делённое на среднюю длину строки; хвост — меньше строки.
        val approximateLine = maxOf(1, charsPerScreen / ESTIMATED_LINES_PER_SCREEN)
        val remainder = paragraph.length % approximateLine
        return if (remainder == 0) 0 else approximateLine - remainder
    }

    /**
     * Номер экрана, на котором лежит позиция.
     *
     * Именно через границы, а не делением смещения на длину экрана:
     * экраны неравной длины, потому что абзацы не режутся по живому.
     */
    fun screenOf(screenStarts: List<Int>, charOffset: Int): Int {
        if (screenStarts.isEmpty()) return 0
        val index = screenStarts.indexOfLast { it <= charOffset }
        return if (index < 0) 0 else index
    }

    /**
     * Оценка вместимости экрана в символах.
     *
     * Считается по площади: сколько строк влезает по высоте и сколько
     * символов в строке по ширине. Средняя ширина символа берётся долей
     * от размера шрифта — для кириллицы и латиницы это около 0.5em, и
     * ошибка здесь не критична: она смещает границы экранов, но не
     * теряет и не повторяет текст, потому что позиция всё равно
     * хранится в символах.
     */
    fun estimateCharsPerScreen(
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        fontSizePx: Float,
        lineHeightMultiplier: Float,
        horizontalPaddingPx: Int,
    ): Int {
        if (viewportWidthPx <= 0 || viewportHeightPx <= 0 || fontSizePx <= 0f) return 0
        val textWidth = viewportWidthPx - 2 * horizontalPaddingPx
        if (textWidth <= 0) return 0
        val lineHeight = fontSizePx * lineHeightMultiplier
        if (lineHeight <= 0f) return 0
        val lines = (viewportHeightPx / lineHeight).toInt()
        val charsPerLine = (textWidth / (fontSizePx * AVERAGE_CHAR_WIDTH_EM)).toInt()
        return maxOf(1, lines * charsPerLine)
    }

    private const val AVERAGE_CHAR_WIDTH_EM = 0.5f
    private const val ESTIMATED_LINES_PER_SCREEN = 20
}
