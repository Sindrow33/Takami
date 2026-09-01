package core.store

import core.model.ConfigOrigin
import core.model.RepairProposal
import core.model.SourceConfig
import core.validate.ContractReport
import core.validate.SourceHealth
import core.validate.Verdict
import java.io.File

@kotlinx.serialization.Serializable
enum class RevisionState { PROBATION, ACTIVE, ROLLED_BACK }

/**
 * Снимок метрик на момент начала испытательного срока — база для сравнения.
 *
 * Тонкость, стоившая одного ложного откатa: сравнивать починку надо НЕ
 * с тем, как источник работал до поломки. Тот конфиг уже мёртв,
 * вернуться к нему нельзя, и требовать от ремонта прежних 1.00 значит
 * гарантированно откатывать любую рабочую починку с 0.90. Альтернатива
 * ревизии — не идеальное прошлое, а сломанный конфиг здесь и сейчас;
 * именно его метрики и лежат в emaFill/emaConfidence. Показатели
 * прошлого остаются в lastGoodCount: количество элементов подделать
 * нечем, и падение вдвое — по-прежнему повод для отката.
 */
@kotlinx.serialization.Serializable
data class Baseline(
    val successRate: Double = 1.0,
    /** Заполненность СЛОМАННОГО конфига, который ревизия заменяет. */
    val emaFill: Double = 0.0,
    /** Уверенность СЛОМАННОГО конфига, который ревизия заменяет. */
    val emaConfidence: Double = 0.0,
    /** Число элементов на здоровой истории — защита от селектора-пустышки. */
    val lastGoodCount: Int = 0,
)

@kotlinx.serialization.Serializable
data class Observations(
    val runs: Int = 0,
    val broken: Int = 0,
    val degraded: Int = 0,
    val fillSum: Double = 0.0,
    val confidenceSum: Double = 0.0,
    val countSum: Int = 0,
) {
    val avgFill: Double get() = if (runs == 0) 0.0 else fillSum / runs
    val avgConfidence: Double get() = if (runs == 0) 0.0 else confidenceSum / runs
    val avgCount: Double get() = if (runs == 0) 0.0 else countSum.toDouble() / runs
}

@kotlinx.serialization.Serializable
data class Revision(
    val id: String,
    val host: String,
    val config: SourceConfig,
    val state: RevisionState,
    val createdAt: Long,
    val baseline: Baseline,
    val observations: Observations = Observations(),
    val selectors: List<String> = emptyList(),
    val note: String = "",
)

@kotlinx.serialization.Serializable
data class Vote(
    val field: String,
    val selector: String,
    val confidence: Double,
    val at: Long,
)

@kotlinx.serialization.Serializable
data class RevisionBook(
    val host: String,
    val active: Revision? = null,
    val probation: Revision? = null,
    val history: List<Revision> = emptyList(),
    /** селектор → до какого времени запрещён */
    val blocklist: Map<String, Long> = emptyMap(),
    val votes: List<Vote> = emptyList(),
)

class RevisionStore(dir: File) : JsonStore<RevisionBook>(dir) {
    override fun encode(value: RevisionBook) = json.encodeToString(RevisionBook.serializer(), value)
    override fun decode(text: String) = json.decodeFromString(RevisionBook.serializer(), text)
    override fun keyOf(value: RevisionBook) = value.host
    fun book(host: String): RevisionBook = load(host) ?: RevisionBook(host)
}

sealed interface Decision {
    data class Keep(val left: Int) : Decision
    data class Finalize(val id: String) : Decision
    data class Rollback(val id: String, val reason: String) : Decision
    data object Nothing : Decision
}

/**
 * Ревизии конфига: голоса → испытательный срок → закрепление или откат.
 * Ничего не чинит сам, только решает, доверять ли предложенному.
 */
class RevisionManager(
    private val store: RevisionStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Голос принимается только от достаточно уверенного ремонта. */
    fun vote(
        host: String,
        base: SourceConfig,
        proposals: List<RepairProposal>,
        health: SourceHealth,
        /** Отчёт о поломке: его метрики и становятся базой сравнения. */
        broken: ContractReport? = null,
    ): Boolean {
        var book = store.book(host)
        if (book.probation != null) return false

        val fresh = proposals.filter {
            it.confidence >= VOTE_MIN_CONFIDENCE && !isBlocked(book, it.selector)
        }
        if (fresh.isEmpty()) return false

        book = book.copy(votes = book.votes + fresh.map { Vote(it.field, it.selector, it.confidence, now()) })

        // Кворум считается по полю: одно и то же исправление должно прийти
        // независимо несколько раз, иначе это случайность одной страницы.
        val agreed = book.votes
            .groupBy { it.field to it.selector }
            .filterValues { it.size >= QUORUM }

        if (agreed.isEmpty()) { store.save(book); return false }

        val repairs = agreed.entries.associate { (k, v) ->
            k.first to RepairProposal(k.first, k.second, v.maxOf { it.confidence })
        }
        val id = "rev-${now()}-${repairs.keys.sorted().joinToString("+").take(24)}"
        val rev = Revision(
            id = id,
            host = host,
            config = base.withRepairs(repairs).copy(origin = ConfigOrigin.HEALED_PROBATION),
            state = RevisionState.PROBATION,
            createdAt = now(),
            baseline = baselineOf(health, broken),
            selectors = repairs.values.map { it.selector },
            note = "кворум по полям ${repairs.keys.sorted()}",
        )
        store.save(book.copy(probation = rev, votes = emptyList()))
        return true
    }

    /** Запоминает метрики того состояния, которое ревизия заменяет. */
    fun armBaseline(host: String, health: SourceHealth, broken: ContractReport? = null) {
        val book = store.book(host)
        val p = book.probation ?: return
        store.save(book.copy(probation = p.copy(baseline = baselineOf(health, broken))))
    }

    private fun baselineOf(health: SourceHealth, broken: ContractReport?): Baseline {
        if (broken == null) return Baseline(
            successRate = health.successRate,
            emaFill = health.emaFill,
            emaConfidence = health.emaConfidence,
            lastGoodCount = health.lastGoodCount,
        )
        val brokenFill = broken.fillRates.values.average().takeIf { !it.isNaN() } ?: 0.0
        return Baseline(
            successRate = health.successRate,
            emaFill = brokenFill,
            emaConfidence = broken.verdict.confidenceOrZero,
            lastGoodCount = health.lastGoodCount,
        )
    }

    fun observe(host: String, report: ContractReport): Decision {
        val book = store.book(host)
        val p = book.probation ?: return Decision.Nothing
        val v = report.verdict

        val o = p.observations.let {
            it.copy(
                runs = it.runs + 1,
                broken = it.broken + if (v is Verdict.Broken) 1 else 0,
                degraded = it.degraded + if (v is Verdict.Degraded) 1 else 0,
                fillSum = it.fillSum + (report.fillRates.values.average().takeIf { d -> !d.isNaN() } ?: 0.0),
                confidenceSum = it.confidenceSum + v.confidenceOrZero,
                countSum = it.countSum + report.itemCount,
            )
        }
        val upd = p.copy(observations = o)

        hardBreach(upd)?.let { return rollback(book, upd, it) }
        if (o.runs < PROBATION_MIN_RUNS) {
            store.save(book.copy(probation = upd))
            return Decision.Keep(PROBATION_MIN_RUNS - o.runs)
        }
        softBreach(upd)?.let { return rollback(book, upd, it) }

        val done = upd.copy(state = RevisionState.ACTIVE)
        store.save(book.copy(
            active = done,
            probation = null,
            history = (book.history + listOfNotNull(book.active)).takeLast(HISTORY_CAP),
        ))
        return Decision.Finalize(done.id)
    }

    /** Немедленный откат: ждать конца срока незачем. */
    private fun hardBreach(r: Revision): String? {
        val o = r.observations
        return when {
            o.broken >= 2 -> "два разбора подряд сломаны"
            o.runs >= 3 && o.avgCount < r.baseline.lastGoodCount * 0.5 && r.baseline.lastGoodCount > 0 ->
                "элементов вдвое меньше обычного (%.1f против %d)".format(o.avgCount, r.baseline.lastGoodCount)
            else -> null
        }
    }

    /** Итог испытательного срока: сравнение с базой. */
    private fun softBreach(r: Revision): String? {
        val o = r.observations
        val b = r.baseline
        // Без снимка "до" сравнивать не с чем: судим по абсолютным порогам.
        if (b.emaConfidence <= 0.0 && b.emaFill <= 0.0) {
            return when {
                o.avgConfidence < BLIND_MIN_CONFIDENCE ->
                    "уверенность %.2f без базы сравнения".format(o.avgConfidence)
                o.degraded.toDouble() / o.runs > MAX_DEGRADED_SHARE ->
                    "деградаций ${o.degraded} из ${o.runs} без базы сравнения"
                else -> null
            }
        }
        return when {
            b.emaConfidence - o.avgConfidence > TOLERANCE_CONFIDENCE ->
                "уверенность %.2f против %.2f".format(o.avgConfidence, b.emaConfidence)
            b.emaFill - o.avgFill > TOLERANCE_FILL ->
                "заполненность %.2f против %.2f".format(o.avgFill, b.emaFill)
            o.degraded.toDouble() / o.runs > MAX_DEGRADED_SHARE ->
                "деградаций ${o.degraded} из ${o.runs}"
            else -> null
        }
    }

    private fun rollback(book: RevisionBook, r: Revision, reason: String): Decision {
        val until = now() + BLOCK_DAYS * 86_400_000L
        store.save(book.copy(
            probation = null,
            history = (book.history + r.copy(state = RevisionState.ROLLED_BACK, note = reason)).takeLast(HISTORY_CAP),
            blocklist = book.blocklist + r.selectors.associateWith { until },
        ))
        return Decision.Rollback(r.id, reason)
    }

    fun effectiveConfig(host: String, bundled: SourceConfig): SourceConfig =
        store.book(host).let { it.probation?.config ?: it.active?.config ?: bundled }

    fun isBlocked(host: String, selector: String) = isBlocked(store.book(host), selector)

    private fun isBlocked(book: RevisionBook, selector: String) =
        (book.blocklist[selector] ?: 0L) > now()

    private companion object {
        const val QUORUM = 3
        const val VOTE_MIN_CONFIDENCE = 0.70
        const val PROBATION_MIN_RUNS = 10
        const val TOLERANCE_CONFIDENCE = 0.10
        const val TOLERANCE_FILL = 0.07
        const val MAX_DEGRADED_SHARE = 0.30
        const val BLOCK_DAYS = 30
        const val BLIND_MIN_CONFIDENCE = 0.75
        const val HISTORY_CAP = 8
    }
}
