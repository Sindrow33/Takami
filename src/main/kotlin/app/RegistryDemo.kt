package app

import core.model.RepairProposal
import core.store.Outcome
import core.store.SourceRegistry
import core.test.CatalogPage
import core.test.Fixtures
import core.test.Harness
import java.io.File

fun main() {
    val root = File(System.getProperty("java.io.tmpdir"), "autoheal-registry")
    root.deleteRecursively()
    val bundled = Fixtures.catalogConfig()
    val host = bundled.host
    val harness = Harness()

    fun session(label: String, runs: Int, propose: Boolean = false, html: () -> String) {
        // Новый экземпляр = имитация перезапуска приложения.
        val reg = SourceRegistry(root)
        println("--- $label (реестр создан заново) ---")
        if (propose) repeat(3) {
            reg.proposeRepair(host, bundled, listOf(
                RepairProposal("__item", "div.catalog-item, div.rec-card", 0.88)))
        }
        repeat(runs) { i ->
            val cfg = reg.config(host, bundled)
            val out = harness.run(html(), cfg, reg.health(host).takeIf { it.successCount > 0 })
            when (val o = reg.record(host, out.report, 8000)) {
                is Outcome.Fine -> {}
                is Outcome.Healed -> println("  прогон ${i + 1}: ревизия закреплена")
                is Outcome.RolledBack -> println("  прогон ${i + 1}: откат — ${o.reason}")
                is Outcome.AskUser -> println("  прогон ${i + 1}: нужен пользователь — ${o.reason}")
            }
        }
        val h = reg.health(host)
        println("  итог: ok=${h.successCount} deg=${h.degradedCount} fail=${h.failureCount} " +
                "rate=%.2f emaConf=%.2f".format(h.successRate, h.emaConfidence))
    }

    session("день 1: норма", 6) { CatalogPage().html() }
    session("день 2: норма, память подхвачена", 6) { CatalogPage().html() }
    session("день 3: ремонт захватил рекомендации", 12, propose = true) {
        CatalogPage(withRecommendations = true).html()
    }
    session("день 4: после отката", 6) { CatalogPage().html() }

    println("\nфайлы состояния:")
    root.walkTopDown().filter { it.isFile }.forEach {
        println("  ${it.relativeTo(root)}  ${it.length()} B")
    }
}
