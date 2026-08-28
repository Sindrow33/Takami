package core.extract

import core.model.ParsedPayload

/**
 * Диагностика одного прохода извлечения: какой ступенью лестницы селекторов
 * пришлось воспользоваться и что потерялось по дороге.
 */
data class ExtractionTrace(
    /** 0 — сработал основной селектор карточек, >0 — номер запасного. */
    val itemRung: Int = 0,
    /** Худшая использованная ступень по каждому полю. */
    val fieldRungs: Map<String, Int> = emptyMap(),
    /** Поля, не давшие ни одного значения. */
    val emptyFields: Set<String> = emptySet(),
    /** Сколько ключей повторилось (не схлопываем, отдаём валидатору). */
    val duplicateKeys: Int = 0,
    /** Карточки, выброшенные из-за отсутствия url или title. */
    val droppedItems: Int = 0,
) {
    val usedFallback: Boolean
        get() = itemRung > 0 || fieldRungs.values.any { it > 0 }
}

/** Полезная нагрузка вместе со следом её добычи. */
data class Extracted(
    val payload: ParsedPayload,
    val trace: ExtractionTrace,
)
