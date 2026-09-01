package core.store

import core.model.RepairProposal
import core.model.SourceConfig
import core.validate.ContractReport
import core.validate.ErrorBudget
import core.validate.SourceHealth
import core.validate.Verdict
import java.io.File

@kotlinx.serialization.Serializable
private data class BudgetWindow(val host: String, val weights: List<Double> = emptyList())

private class BudgetStore(dir: File) : JsonStore<BudgetWindow>(dir) {
    override fun encode(value: BudgetWindow) = json.encodeToString(BudgetWindow.serializer(), value)
    override fun decode(text: String) = json.decodeFromString(BudgetWindow.serializer(), text)
    override fun keyOf(value: BudgetWindow) = value.host
}

/** Что делать вызывающему после разбора. */
sealed interface Outcome {
    data class Fine(val report: ContractReport) : Outcome
    data class Healed(val revision: String) : Outcome
    data class RolledBack(val reason: String) : Outcome
    data class AskUser(val reason: String) : Outcome
}

/**
 * Единая точка входа в состояние источника: конфиг, здоровье, ревизии, бюджет.
 * Порядок действий после разбора живёт здесь, а не у вызывающего.
 */
class SourceRegistry(root: File) {

    private val configs = ConfigStore(File(root, "config"))
    private val healths = HealthStore(File(root, "health"))
    private val revisions = RevisionStore(File(root, "revision"))
    private val budgets = BudgetStore(File(root, "budget"))
    private val manager = RevisionManager(revisions)
    private val budget = ErrorBudget()

    /** Окно бюджета переживает перезапуск: иначе оно всегда пустое. */
    private fun warmBudget(host: String) {
        if (warmed.add(host)) budgets.load(host)?.weights?.forEach { budget.replay(host, it) }
    }
    private val warmed = HashSet<String>()

    private fun coolBudget(host: String) =
        budgets.save(BudgetWindow(host, budget.snapshot(host)))

    fun health(host: String): SourceHealth = healths.loadOrFresh(host)

    /** Селектор, отвергнутый откатом: повторно предлагать его нельзя. */
    fun isBlocked(host: String, selector: String): Boolean = manager.isBlocked(host, selector)

    fun config(host: String, bundled: SourceConfig): SourceConfig =
        manager.effectiveConfig(host, configs.load(host) ?: bundled)

    /**
     * Записать конфиг на диск. Вызывается на `configToPersist` из
     * разбора: без этого отремонтированные селекторы и снятые отпечатки
     * живут только до конца процесса, и следующий запуск лечится с нуля.
     */
    fun persist(config: SourceConfig) = configs.save(config)

    fun proposeRepair(
        host: String,
        base: SourceConfig,
        proposals: List<RepairProposal>,
        broken: ContractReport? = null,
    ): Boolean = manager.vote(host, base, proposals, health(host), broken)

    /** Единственный путь, которым состояние источника меняется после разбора. */
    fun record(host: String, report: ContractReport, bodySize: Int, browserHeaders: Boolean = false): Outcome {
        warmBudget(host)
        val before = health(host)

        val after = when (report.verdict) {
            is Verdict.Ok -> { budget.recordSuccess(host); before.onSuccess(report, bodySize, browserHeaders) }
            is Verdict.Degraded -> { budget.recordDegraded(host); before.onPartial(report, bodySize) }
            is Verdict.Broken -> { budget.recordFailure(host); before.onFailure() }
            is Verdict.EmptyByDesign -> before
        }
        healths.save(after)
        coolBudget(host)

        return when (val d = manager.observe(host, report)) {
            is Decision.Rollback -> {
                // Откат снимает накопленную статистику: она относилась к отвергнутому конфигу.
                budget.reset(host)
                coolBudget(host)
                Outcome.RolledBack(d.reason)
            }
            is Decision.Finalize -> Outcome.Healed(d.id)
            else ->
                if (budget.isExhausted(host)) Outcome.AskUser("бюджет ошибок исчерпан")
                else Outcome.Fine(report)
        }
    }
}
