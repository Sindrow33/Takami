package core.engine

import core.store.SourceRegistry
import core.validate.SourceHealth

/**
 * Сводка для UI-индикатора автопарсера. Экран не должен знать ни про
 * ревизии, ни про бюджет ошибок — ему нужны четыре числа и лог.
 * Всё считается из уже сохранённого состояния, без сети и разбора.
 */
data class ParserStats(
    /** Обучаемость: то самое «72%» в шапке главной. */
    val learningPercent: Int,
    val sourceCount: Int,
    val selfHealCount: Int,
    val accuracyPercent: Int,
    val anomalyCount: Int,
    val log: List<LogEntry>,
) {
    data class LogEntry(val host: String, val message: String, val tone: Tone, val atMillis: Long)
    enum class Tone { OK, WARN, ERROR }

    companion object {
        val EMPTY = ParserStats(0, 0, 0, 0, 0, emptyList())
    }
}

class ParserStatsProvider(private val registry: SourceRegistry) {

    fun stats(hosts: List<String>): ParserStats {
        if (hosts.isEmpty()) return ParserStats.EMPTY
        val healths = hosts.map { registry.health(it) }

        val accuracy = healths.map { it.successRate }.average()
        val anomalies = healths.count { it.failStreak > 0 || it.successRate < 0.9 }

        /*
         * Обучаемость — не абстрактный прогресс, а доля источников,
         * по которым накопилось достаточно истории, чтобы валидатор
         * мог отличать поломку от нормы. Без истории все проверки
         * вырождаются, поэтому именно она и есть «обученность».
         */
        val learning = healths.map { maturity(it) }.average()

        return ParserStats(
            learningPercent = (learning * 100).toInt().coerceIn(0, 100),
            sourceCount = hosts.size,
            selfHealCount = healths.sumOf { it.degradedCount },
            accuracyPercent = (accuracy * 100).toInt().coerceIn(0, 100),
            anomalyCount = anomalies,
            log = healths.flatMap { logOf(it) }.sortedByDescending { it.atMillis }.take(12),
        )
    }

    /** Зрелость одного источника: история прогонов + стабильные поля. */
    private fun maturity(h: SourceHealth): Double {
        val runs = (h.successCount / MATURE_RUNS.toDouble()).coerceAtMost(1.0)
        val fields = if (h.stableFields.isEmpty()) 0.0 else 1.0
        return runs * 0.7 + fields * 0.15 + h.emaConfidence * 0.15
    }

    private fun logOf(h: SourceHealth): List<ParserStats.LogEntry> = buildList {
        if (h.lastFailureAt > 0 && h.failStreak > 0) add(
            ParserStats.LogEntry(h.host, "разбор не удался ${h.failStreak} раз подряд",
                ParserStats.Tone.ERROR, h.lastFailureAt)
        )
        if (h.degradedCount > 0 && h.lastSuccessAt > 0) add(
            ParserStats.LogEntry(h.host, "структура изменилась, конфиг восстановлен",
                ParserStats.Tone.OK, h.lastSuccessAt)
        )
        if (h.lastSuccessAt > 0 && h.failStreak == 0 && h.degradedCount == 0) add(
            ParserStats.LogEntry(h.host, "${h.lastGoodCount} элементов, без замечаний",
                ParserStats.Tone.OK, h.lastSuccessAt)
        )
    }

    private companion object { const val MATURE_RUNS = 40 }
}
