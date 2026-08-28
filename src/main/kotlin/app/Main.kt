package app

import core.model.SourceConfig
import core.test.CatalogPage
import core.test.Fixtures
import core.test.Harness
import core.test.RunOutcome
import core.validate.ContractReport
import core.validate.Severity
import core.validate.SourceHealth
import core.validate.Verdict

private data class Expect(
    val verdict: String,
    val items: Int,
    val confidence: ClosedFloatingPointRange<Double>? = null,
    val mustHave: Set<String> = emptySet(),
    val mustNotHave: Set<String> = emptySet(),
)

private class Scenario(
    val id: String,
    val title: String,
    val html: () -> String,
    val config: () -> SourceConfig = { Fixtures.catalogConfig() },
    val health: () -> SourceHealth = { Fixtures.healthyHistory() },
    val expect: Expect,
)

private val scenarios = listOf(
    Scenario(
        id = "S1",
        title = "База: каталог без изменений",
        html = { CatalogPage().html() },
        expect = Expect(verdict = "Ok", items = 24, confidence = 0.90..1.0),
    ),
    Scenario(
        id = "S2",
        title = "Редизайн: классы заменены на хеши",
        html = {
            CatalogPage(itemClass = "css-1q7hf2n", titleClass = "css-9klm3a").html()
        },
        expect = Expect(
            verdict = "Broken",
            items = 0,                       // ожидаем, что мусор из фолбэка не пройдёт
        ),
    ),
    Scenario(
        id = "S3",
        title = "Блок рекомендаций подмешан в каталог",
        html = { CatalogPage(withRecommendations = true).html() },
        expect = Expect(
            verdict = "Degraded",            // проверяем гипотезу про историческую заполненность
            items = 32,
            mustHave = setOf("FIELD_FILL_DROP"),
        ),
    ),
    Scenario(
        id = "S4",
        title = "Пустая выдача у нового источника",
        html = { CatalogPage(count = 0, withNav = false).html() },
        health = { Fixtures.freshHistory() },
        expect = Expect(verdict = "EmptyByDesign", items = 0),
    ),
    Scenario(
        id = "S4b",
        title = "Пустая выдача у здорового источника",
        html = { CatalogPage(count = 0, withNav = false).html() },
        expect = Expect(verdict = "Broken", items = 0, mustHave = setOf("LIST_EMPTY")),
    ),
    Scenario(
        id = "S5",
        title = "Дубликаты url во всех карточках",
        html = { CatalogPage(duplicateUrls = true).html() },
        expect = Expect(
            verdict = "Broken",
            items = 24,
            mustHave = setOf("KEY_DUPLICATE", "FIELD_CONSTANT"),
        ),
    ),
)

fun main() {
    val harness = Harness()
    var failed = 0

    for (s in scenarios) {
        val html = s.html()
        val outcome = runCatching { harness.run(html, s.config(), s.health()) }

        println("─".repeat(72))
        println("${s.id}  ${s.title}")
        println("html: ${html.length} B")

                val err = outcome.exceptionOrNull()
        if (err != null) {
            failed++
            println("  ИСКЛЮЧЕНИЕ: ${err::class.simpleName}: ${err.message}")
            err.stackTrace.take(6).forEach { println("    at $it") }
            continue
        }
        val out = outcome.getOrThrow()

        printOutcome(out)
        val problems = check(out, s.expect)
        if (problems.isEmpty()) {
            println("  ✔ соответствует ожиданию")
        } else {
            failed++
            problems.forEach { println("  ✘ $it") }
        }
    }

    println("─".repeat(72))
    println(if (failed == 0) "все сценарии совпали с ожиданием"
            else "расхождений: $failed из ${scenarios.size}")
}

private fun printOutcome(out: RunOutcome) {
    val r: ContractReport = out.report
    println("  verdict:    ${describe(r.verdict)}")
    println("  elements:   ${r.itemCount}")
    if (r.fillRates.isNotEmpty()) {
        val line = r.fillRates.entries
            .sortedBy { it.key }
            .joinToString("  ") { "${it.key}=%.2f".format(it.value) }
        println("  fill:       $line")
    }
    if (r.issues.isEmpty()) {
        println("  issues:     —")
    } else {
        println("  issues:")
        r.issues.sortedByDescending { it.severity.ordinal }.forEach {
            val f = it.field?.let { n -> " [$n]" } ?: ""
            val p = "%.2f".format(it.penalty)
            println("    ${pad(it.severity)} ${it.code}$f  p=$p  ${it.message}")
        }
    }
    r.trace?.let { t ->
        println("  trace:      fallback=${t.usedFallback} dropped=${t.droppedItems} dupes=${t.duplicateKeys}")
    }
}

private fun pad(s: Severity) = s.name.padEnd(5)

private fun describe(v: Verdict): String = when (v) {
    is Verdict.Ok -> "Ok(confidence=%.2f)".format(v.confidence)
    is Verdict.Degraded -> "Degraded(confidence=%.2f, broken=%s)".format(v.confidence, v.brokenFields)
    is Verdict.Broken -> "Broken(broken=${v.brokenFields}, reason=${v.reason})"
    is Verdict.EmptyByDesign -> "EmptyByDesign(reason=${v.reason})"
}

private fun nameOf(v: Verdict): String = when (v) {
    is Verdict.Ok -> "Ok"
    is Verdict.Degraded -> "Degraded"
    is Verdict.Broken -> "Broken"
    is Verdict.EmptyByDesign -> "EmptyByDesign"
}

private fun confidenceOf(v: Verdict): Double? = when (v) {
    is Verdict.Ok -> v.confidence
    is Verdict.Degraded -> v.confidence
    else -> null
}

private fun check(out: RunOutcome, e: Expect): List<String> {
    val problems = mutableListOf<String>()
    val r = out.report
    val actual = nameOf(r.verdict)
    if (actual != e.verdict) problems += "verdict: ждали ${e.verdict}, получили $actual"
    if (r.itemCount != e.items) problems += "elements: ждали ${e.items}, получили ${r.itemCount}"
    e.confidence?.let { range ->
        val c = confidenceOf(r.verdict)
        if (c == null) problems += "confidence: вердикт без числа, ждали $range"
        else if (c !in range) problems += "confidence: ждали $range, получили %.2f".format(c)
    }
    val codes = r.issues.map { it.code.name }.toSet()
    (e.mustHave - codes).forEach { problems += "нет ожидаемого issue $it" }
    (e.mustNotHave intersect codes).forEach { problems += "лишний issue $it" }
    return problems
}
