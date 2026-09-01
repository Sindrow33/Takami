package core

import core.engine.ParserStats
import core.engine.ParserStatsProvider
import core.test.CatalogPage
import core.test.EngineHarness
import core.test.Fixtures
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ParserStatsTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test fun `без источников сводка пустая, а не выдуманная`() {
        val h = EngineHarness(tmp.newFolder("empty"))
        val s = ParserStatsProvider(h.registry).stats(emptyList())
        assertEquals(ParserStats.EMPTY, s)
        assertEquals(0, s.learningPercent)
    }

    @Test fun `обучаемость растёт с историей прогонов`() {
        val h = EngineHarness(tmp.newFolder("grow"))
        val config = Fixtures.catalogConfig()
        val provider = ParserStatsProvider(h.registry)

        h.run(CatalogPage().html(), config)
        val early = provider.stats(listOf(config.host)).learningPercent
        repeat(12) { h.run(CatalogPage().html(), config) }
        val later = provider.stats(listOf(config.host)).learningPercent

        assertTrue("обучаемость должна вырасти: $early -> $later", later > early)
        assertTrue(later in 0..100)
    }

    @Test fun `точность и аномалии отражают реальные поломки`() {
        val h = EngineHarness(tmp.newFolder("acc"))
        val config = Fixtures.catalogConfig()
        val provider = ParserStatsProvider(h.registry)

        repeat(6) { h.run(CatalogPage().html(), config) }
        val clean = provider.stats(listOf(config.host))
        assertEquals(0, clean.anomalyCount)
        assertTrue("точность на чистых прогонах: ${clean.accuracyPercent}", clean.accuracyPercent >= 90)

        // Источник с историей внезапно отдаёт пустоту — это поломка.
        h.run(CatalogPage(count = 0).html(), config)
        val broken = provider.stats(listOf(config.host))
        assertEquals(1, broken.anomalyCount)
        assertTrue("точность обязана просесть: ${broken.accuracyPercent}", broken.accuracyPercent < clean.accuracyPercent)
    }

    @Test fun `лог не пустой после прогонов`() {
        val h = EngineHarness(tmp.newFolder("log"))
        val config = Fixtures.catalogConfig()
        repeat(3) { h.run(CatalogPage().html(), config) }
        val s = ParserStatsProvider(h.registry).stats(listOf(config.host))
        assertTrue("в логе должна быть хотя бы одна запись", s.log.isNotEmpty())
        assertEquals(config.host, s.log.first().host)
    }
}
