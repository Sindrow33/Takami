package core

import core.store.Decision
import core.store.Outcome
import core.test.CatalogPage
import core.test.EngineHarness
import core.test.Fixtures
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Полный контур: движок сам разбирает, ловит поломку, голосует за ремонт,
 * набирает кворум, проводит испытательный срок и закрепляет ревизию.
 * Состояние живёт на диске — как в бою.
 */
class EngineLoopTest {

    @get:Rule val tmp = TemporaryFolder()

    private val healthy = CatalogPage().html()
    private val redesigned = CatalogPage(itemClass = "css-1q7hf2n", titleClass = "css-9klm3a").html()

    @Test fun `здоровые прогоны накапливают отпечатки и историю`() {
        val h = EngineHarness(tmp.newFolder("ok"))
        var config = Fixtures.catalogConfig()

        repeat(3) {
            val r = h.run(healthy, config)
            assertTrue("прогон ${it + 1}: ${r.report.verdict}", r.isUsable)
            r.configToPersist?.let { config = it }
        }

        val title = config.listing!!.fields["title"]!!
        assertNotNull("отпечаток должен накопиться за здоровые прогоны", title.signature)
        assertTrue("подтверждений: ${title.signature!!.confirmed}", title.signature!!.confirmed >= 2)

        val health = h.registry.health(config.host)
        assertEquals(24, health.lastGoodCount)
        assertTrue("стабильные поля: ${health.stableFields}", "title" in health.stableFields)
    }

    @Test fun `поломка порождает предложения ремонта`() {
        val h = EngineHarness(tmp.newFolder("broken"))
        var config = Fixtures.catalogConfig()
        repeat(2) { h.run(healthy, config).configToPersist?.let { config = it } }

        val r = h.run(redesigned, config)
        assertFalse("редизайн обязан быть замечен", r.isUsable)
        assertNotNull(r.heal)
        assertFalse("ремонт обязан что-то предложить", r.heal!!.isEmpty)
    }

    @Test fun `кворум из трёх поломок поднимает ревизию на испытательный срок`() {
        val h = EngineHarness(tmp.newFolder("quorum"))
        var config = Fixtures.catalogConfig()
        repeat(2) { h.run(healthy, config).configToPersist?.let { config = it } }

        // Три независимых прогона со сломанной вёрсткой — ровно QUORUM.
        repeat(3) { h.run(redesigned, config) }

        val effective = h.registry.config(config.host, config)
        assertNotEquals(
            "после кворума в ход должен пойти новый селектор карточки",
            config.listing!!.itemSelector,
            effective.listing!!.itemSelector,
        )
        assertEquals(
            "ревизия обязана быть на испытательном сроке, а не сразу активной",
            core.model.ConfigOrigin.HEALED_PROBATION,
            effective.origin,
        )
    }

    @Test fun `ревизия с рабочим селектором закрепляется после испытательного срока`() {
        val h = EngineHarness(tmp.newFolder("finalize"))
        var config = Fixtures.catalogConfig()
        repeat(2) { h.run(healthy, config).configToPersist?.let { config = it } }
        repeat(3) { h.run(redesigned, config) }

        // Испытательный срок: PROBATION_MIN_RUNS прогонов на новой вёрстке.
        var healed = false
        repeat(12) {
            val r = h.run(redesigned, config)
            if (r.outcome is Outcome.Healed) healed = true
        }

        assertTrue("ревизия должна закрепиться, раз новый селектор работает", healed)

        // После закрепления разбор обязан оставаться рабочим и дальше.
        val after = h.run(redesigned, config)
        assertTrue("после закрепления: ${after.report.verdict}", after.isUsable)
        assertEquals(24, after.report.itemCount)
    }

    @Test fun `разбор по починенному конфигу снова даёт элементы`() {
        val h = EngineHarness(tmp.newFolder("recover"))
        var config = Fixtures.catalogConfig()
        repeat(2) { h.run(healthy, config).configToPersist?.let { config = it } }
        repeat(3) { h.run(redesigned, config) }

        val r = h.run(redesigned, config)
        assertTrue(
            "после применения ревизии каталог обязан читаться: ${r.report.verdict}, ${r.report.itemCount} элементов",
            r.report.itemCount >= 20,
        )
        assertTrue("вердикт после починки: ${r.report.verdict}", r.isUsable)
    }

    /**
     * Обратная сторона: если предложенный селектор мусорный, испытательный
     * срок обязан его отбросить. База сравнения — сломанный конфиг, но
     * защита по числу элементов работает от здоровой истории.
     */
    @Test fun `пустышка-селектор откатывается по числу элементов`() {
        val h = EngineHarness(tmp.newFolder("rollback"))
        val config = Fixtures.catalogConfig()
        repeat(3) { h.run(healthy, config) }

        // Ревизия, ведущая на футер: селектор валиден, элементов почти нет.
        val bogus = listOf(core.model.RepairProposal(core.model.SourceConfig.ITEM_KEY, "footer div.list > div", 0.9))
        repeat(3) { h.registry.proposeRepair(config.host, config, bogus) }

        var rolled: String? = null
        repeat(6) {
            val r = h.run(healthy, config)
            (r.outcome as? Outcome.RolledBack)?.let { rb -> rolled = rb.reason }
        }
        assertNotNull("мусорная ревизия обязана быть откачена", rolled)

        // После откатa источник возвращается к прежнему конфигу,
        // а мусорный селектор попадает в блоклист.
        assertTrue("селектор обязан быть заблокирован", h.registry.isBlocked(config.host, "footer div.list > div"))
        val back = h.run(healthy, config)
        assertTrue("после откатa разбор снова рабочий: ${back.report.verdict}", back.isUsable)
        assertEquals(24, back.report.itemCount)
    }

    @Test fun `состояние переживает пересоздание реестра`() {
        val root = tmp.newFolder("persist")
        var config = Fixtures.catalogConfig()
        EngineHarness(root).let { h ->
            repeat(3) { h.run(healthy, config).configToPersist?.let { config = it } }
        }
        // Новый экземпляр на том же каталоге — как после перезапуска приложения.
        val health = EngineHarness(root).registry.health(config.host)
        assertEquals("история обязана сохраниться на диске", 24, health.lastGoodCount)
        assertTrue(health.successCount >= 3)
    }
}
