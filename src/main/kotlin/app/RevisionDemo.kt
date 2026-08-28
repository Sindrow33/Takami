package app

import core.model.RepairProposal
import core.store.Decision
import core.store.RevisionManager
import core.store.RevisionStore
import core.test.CatalogPage
import core.test.Fixtures
import core.test.Harness
import core.validate.SourceHealth
import core.validate.Verdict
import java.io.File

private val harness = Harness()
private val bundled = Fixtures.catalogConfig()
private val host = bundled.host

/** Конфиг «после ремонта»: селекторы переведены на новые хеш-классы. */
private fun healed() = bundled.copy(
    listing = bundled.listing!!.copy(
        itemSelector = "div.css-1q7hf2n",
        fields = bundled.listing!!.fields.mapValues { (n, f) ->
            if (n == "title") f.copy(selector = ".css-9klm3a") else f
        },
    )
)

private fun redesigned() = CatalogPage(itemClass = "css-1q7hf2n", titleClass = "css-9klm3a").html()

private fun scenario(name: String, html: () -> String, repairSelector: String, arm: Boolean = true) {
    val root = File(System.getProperty("java.io.tmpdir"), "autoheal-rev-${name.hashCode()}")
    root.deleteRecursively()
    val mgr = RevisionManager(RevisionStore(File(root, "rev")))
    var health = Fixtures.healthyHistory()

    println("=== $name ===")

    repeat(3) { i ->
        val ok = mgr.vote(host, bundled, listOf(
            RepairProposal("__item", repairSelector, 0.88),
            RepairProposal("title", ".css-9klm3a", 0.91),
        ), if (arm) health else SourceHealth(host))
        println("голос ${i + 1}: ${if (ok) "кворум набран, ревизия создана" else "накоплен"}")
    }
    if (!arm) println("источник без истории: базы для сравнения нет")

    var verdict = "—"
    for (run in 1..14) {
        val cfg = mgr.effectiveConfig(host, bundled)
        val out = harness.run(html(), cfg, health)
        health = when (val v = out.report.verdict) {
            is Verdict.Ok -> health.onSuccess(out.report, 8000, false)
            is Verdict.Degraded -> health.onPartial(out.report, 8000)
            else -> health.onFailure()
        }
        when (val d = mgr.observe(host, out.report)) {
            is Decision.Keep -> {}
            is Decision.Finalize -> { verdict = "закреплена ${d.id.take(28)} на прогоне $run"; break }
            is Decision.Rollback -> { verdict = "откат на прогоне $run: ${d.reason}"; break }
            Decision.Nothing -> {}
        }
    }
    println(if (verdict == "—") "решение не принято за 14 прогонов" else verdict)
    println("селектор после решения: ${mgr.effectiveConfig(host, bundled).listing?.itemSelector}")
    println("в чёрном списке: ${mgr.isBlocked(host, repairSelector)}\n")
}

fun main() {
    scenario("A. верный ремонт после редизайна", ::redesigned, "div.css-1q7hf2n")
    scenario("B. ремонт увёл на дубликаты", { CatalogPage(duplicateUrls = true).html() }, "div.catalog-item")
    scenario("C. ремонт захватил рекомендации",
        { CatalogPage(withRecommendations = true).html() }, "div.catalog-item, div.rec-card")
    scenario("D. новый источник, ремонт захватил рекомендации",
        { CatalogPage(withRecommendations = true).html() }, "div.catalog-item, div.rec-card", arm = false)
}
