package dev.takami.app.parser

import android.content.Context
import core.engine.ParserStats
import core.engine.ParserStatsProvider
import core.store.SourceRegistry
import java.io.File

/**
 * Мост между модулем `:autoheal` и UI. Держит реестр источников на
 * внутреннем диске приложения и отдаёт экрану готовую сводку.
 *
 * Индикатор в шапке главной раньше показывал случайное число — теперь
 * он читает настоящее состояние автопарсера; пока источников нет,
 * честно показывается пустая сводка, а не выдуманный процент.
 */
class ParserState(context: Context) {

    private val root = File(context.filesDir, "sources")
    private val registry = SourceRegistry(root)
    private val provider = ParserStatsProvider(registry)

    /** Хосты, по которым уже есть накопленное состояние. */
    private fun knownHosts(): List<String> =
        File(root, "health")
            .listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.map { it.name.removeSuffix(".json") }
            .orEmpty()

    fun stats(): ParserStats = provider.stats(knownHosts())

    val hasData: Boolean get() = knownHosts().isNotEmpty()
}
