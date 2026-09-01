package com.mangareader.reader.engine.novel

/**
 * Настройки чтения текста. Отдельно от `ReaderSettings` намеренно: у
 * манги и у текста почти нет общих параметров, а сложенные в один тип
 * они дали бы экран настроек, половина которого неприменима к текущему
 * содержимому — ровно та проблема с врущими переключателями, которую мы
 * чинили в читалке манги.
 */
data class NovelSettings(
    val fontSizeSp: Int = DEFAULT_FONT_SIZE_SP,
    val lineHeightMultiplier: Float = DEFAULT_LINE_HEIGHT,
    val horizontalPaddingDp: Int = DEFAULT_PADDING_DP,
    val theme: NovelTheme = NovelTheme.DARK,
    val fontFamily: NovelFont = NovelFont.SERIF,
    /**
     * Постранично или непрерывной прокруткой.
     *
     * Не косметика: в постраничном режиме позиция обязана падать на
     * начало экрана, иначе перелистывание теряет или повторяет строки.
     */
    val paged: Boolean = false,
    /** Выравнивание по ширине. На узком экране даёт «реки» из пробелов. */
    val justify: Boolean = false,
) {
    /**
     * Диапазоны заданы здесь, а не в UI: ползунок, позволяющий выбрать
     * нечитаемый размер, — это тот же врущий элемент управления.
     */
    fun withFontSize(sp: Int) = copy(fontSizeSp = sp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP))

    fun withLineHeight(multiplier: Float) =
        copy(lineHeightMultiplier = multiplier.coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT))

    fun withPadding(dp: Int) = copy(horizontalPaddingDp = dp.coerceIn(MIN_PADDING_DP, MAX_PADDING_DP))

    companion object {
        const val DEFAULT_FONT_SIZE_SP = 18
        const val MIN_FONT_SIZE_SP = 12
        const val MAX_FONT_SIZE_SP = 32

        const val DEFAULT_LINE_HEIGHT = 1.6f
        const val MIN_LINE_HEIGHT = 1.0f
        const val MAX_LINE_HEIGHT = 2.4f

        const val DEFAULT_PADDING_DP = 20
        const val MIN_PADDING_DP = 8
        const val MAX_PADDING_DP = 48

        val DEFAULT = NovelSettings()
    }
}

/**
 * Тема чтения. Цвета живут здесь, а не в токенах Aurora, потому что это
 * не тема приложения: сепия и белый лист нужны при чтении текста и
 * нигде больше, а приложение остаётся тёмным.
 */
enum class NovelTheme(
    val backgroundArgb: Int,
    val textArgb: Int,
    val label: String,
) {
    DARK(0xFF0F1115.toInt(), 0xFFE6E8EC.toInt(), "Тёмная"),

    /**
     * Не чистый чёрный на чистом белом: максимальный контраст на
     * длинном тексте утомляет глаз быстрее, чем слегка приглушённый.
     */
    LIGHT(0xFFFBFBF9.toInt(), 0xFF1A1A1A.toInt(), "Светлая"),

    SEPIA(0xFFF4ECD8.toInt(), 0xFF3B2F20.toInt(), "Сепия"),

    /** Для OLED: настоящий чёрный, пиксели выключены. */
    BLACK(0xFF000000.toInt(), 0xFFC9CDD4.toInt(), "Чёрная");
}

enum class NovelFont(val label: String) {
    /** Засечки: длинный текст с ними читается легче. */
    SERIF("С засечками"),
    SANS("Без засечек"),
    MONO("Моноширинный"),
}
