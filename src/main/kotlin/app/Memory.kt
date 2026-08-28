package app

import core.store.HealthStore
import core.test.CatalogPage
import core.test.Fixtures
import core.test.Harness
import core.validate.ErrorBudget
import core.validate.Verdict
import java.io.File

fun main() {
    val root = File(System.getProperty("java.io.tmpdir"), "autoheal-memory")
    root.deleteRecursively()
    val store = HealthStore(File(root, "health"))
    val harness = Harness()
    val config = Fixtures.catalogConfig()
    val host = config.host
    val budget = ErrorBudget()

    fun day(n: Int, label: String, html: String) {
        val before = store.loadOrFresh(host)
        val out = harness.run(html, config, before.takeIf { it.successCount > 0 })
        val v = out.report.verdict
        val after = when (v) {
            is Verdict.Ok -> { budget.recordSuccess(host); before.onSuccess(out.report, html.length, false) }
            is Verdict.Degraded -> { budget.recordDegraded(host); before.onPartial(out.report, html.length) }
            else -> { budget.recordFailure(host); before.onFailure() }
        }
        store.save(after)
        val codes = out.report.issues.joinToString(",") { it.code.name }.ifEmpty { "—" }
        println("день %2d %-26s %-9s n=%2d  ok=%d deg=%d fail=%d  rate=%.2f emaFill=%.2f emaConf=%.2f%s"
            .format(n, label, v::class.simpleName, out.itemCount,
                after.successCount, after.degradedCount, after.failureCount,
                after.successRate, after.emaFill, after.emaConfidence,
                if (budget.isExhausted(host)) "  ← бюджет исчерпан" else ""))
        if (codes != "—") println("          $codes")
    }

    repeat(4) { day(it + 1, "обычный каталог", CatalogPage().html()) }
    repeat(12) { day(it + 5, "рекомендации подмешаны", CatalogPage(withRecommendations = true).html()) }
    day(17, "обычный каталог", CatalogPage().html())

    val fin = store.loadOrFresh(host)
    println("\nэталон заполненности: " + fin.fieldFill.entries.sortedBy { it.key }
        .joinToString(" ") { "${it.key}=%.2f".format(it.value) })
}
