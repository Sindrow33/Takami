package app

import core.validate.ErrorBudget

private enum class Out { OK, DEG, FAIL }

private fun probe(label: String, n: Int, gen: (Int) -> Out) {
    val b = ErrorBudget()
    val host = "probe"
    var tripped = -1
    for (i in 1..n) {
        when (gen(i)) {
            Out.OK -> b.recordSuccess(host)
            Out.DEG -> b.recordDegraded(host)
            Out.FAIL -> b.recordFailure(host)
        }
        if (tripped < 0 && b.isExhausted(host)) tripped = i
    }
    var recovery = -1
    val stillHot = b.isExhausted(host)
    if (stillHot) {
        for (i in 1..200) {
            b.recordSuccess(host)
            if (!b.isExhausted(host)) { recovery = i; break }
        }
    }
    println("%-34s срабатывание: %-6s снятие: %s".format(
        label,
        if (tripped < 0) "нет" else "$tripped",
        when { tripped < 0 -> "—"; !stillHot -> "до конца прогона"; recovery < 0 -> ">200"; else -> "+$recovery" }))
}

fun main() {
    println("окно 50, цель 0.97, минимум выборки 20, минимум плохих событий 3\n")
    probe("только успехи", 60) { Out.OK }
    probe("деградация каждая 10-я", 60) { if (it % 10 == 0) Out.DEG else Out.OK }
    probe("деградация каждая 5-я", 60) { if (it % 5 == 0) Out.DEG else Out.OK }
    probe("деградация каждая 3-я", 60) { if (it % 3 == 0) Out.DEG else Out.OK }
    probe("сплошные деградации", 60) { Out.DEG }
    probe("отказ каждый 20-й", 60) { if (it % 20 == 0) Out.FAIL else Out.OK }
    probe("отказ каждый 10-й", 60) { if (it % 10 == 0) Out.FAIL else Out.OK }
    probe("отказ каждый 5-й", 60) { if (it % 5 == 0) Out.FAIL else Out.OK }
    probe("сплошные отказы", 60) { Out.FAIL }
    probe("две деградации в начале", 60) { if (it <= 2) Out.DEG else Out.OK }
    probe("один отказ в начале", 60) { if (it == 1) Out.FAIL else Out.OK }
}
