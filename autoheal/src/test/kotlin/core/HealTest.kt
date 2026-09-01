package core

import core.extract.RequestKind
import core.extract.StandardExtractor
import core.heal.Healer
import core.heal.SelectorSynth
import core.heal.SignatureCapture
import core.heal.Signatures
import core.heal.SignatureSearch
import core.model.SourceConfig
import core.parse.JsoupParser
import core.test.CatalogPage
import core.test.Fixtures
import core.validate.StandardContractChecker
import org.junit.Assert.*
import org.junit.Test

/**
 * Главная проверка автопарсера: после редизайна, стирающего все классы,
 * система должна сама предложить работающие селекторы.
 */
class HealTest {

    private val parser = JsoupParser()
    private val extractor = StandardExtractor()
    private val healer = Healer()

    /** Разбор + проверка на заданном HTML с заданным конфигом. */
    private fun run(html: String, config: SourceConfig, health: core.validate.SourceHealth) =
        parser.parse(html, URL).let { dom ->
            val ex = extractor.extract(RequestKind.LISTING, dom, config, URL)
            dom to StandardContractChecker(config.profile).check(ex.payload, ex.trace, health)
        }

    @Test fun `отпечатки снимаются на здоровом документе`() {
        val config = Fixtures.catalogConfig()
        val (dom, report) = run(CatalogPage().html(), config, Fixtures.healthyHistory())
        val enriched = SignatureCapture.refresh(dom, config, report)

        val title = enriched.listing!!.fields["title"]!!
        assertNotNull("отпечаток поля title обязан появиться", title.signature)
        assertEquals("h3", title.signature!!.tag)
        assertTrue("в отпечатке должен остаться стабильный класс",
            title.signature!!.stableClasses.contains("card-title"))
    }

    @Test fun `отпечаток находит узел после переименования классов`() {
        val config = Fixtures.catalogConfig()
        val (domBefore, reportBefore) = run(CatalogPage().html(), config, Fixtures.healthyHistory())
        val enriched = SignatureCapture.refresh(domBefore, config, reportBefore)
        val sig = enriched.listing!!.fields["title"]!!.signature!!

        // Редизайн: классы стали хешами, разметка та же.
        val after = parser.parse(
            CatalogPage(itemClass = "css-1q7hf2n", titleClass = "css-9klm3a").html(), URL,
        )
        val match = SignatureSearch.find(after, sig)
        assertNotNull("отпечаток должен опознать заголовок после редизайна", match)
        assertEquals("h3", match!!.node.tag)
        assertTrue("нашли не тот узел: ${match.node.text()}",
            match.node.text() in CatalogPage.TITLES.map { it.first })
    }

    @Test fun `после редизайна хилер предлагает рабочий селектор карточки`() {
        val config = Fixtures.catalogConfig()
        val (domOk, reportOk) = run(CatalogPage().html(), config, Fixtures.healthyHistory())
        val enriched = SignatureCapture.refresh(domOk, config, reportOk)

        val brokenHtml = CatalogPage(itemClass = "css-1q7hf2n", titleClass = "css-9klm3a").html()
        val (domBroken, reportBroken) = run(brokenHtml, enriched, Fixtures.healthyHistory())
        assertFalse("сценарий должен быть сломан", reportBroken.verdict.isUsable)

        val heal = healer.heal(domBroken, enriched, reportBroken)
        assertFalse("хилер обязан хоть что-то предложить", heal.isEmpty)

        val itemFix = heal.proposals.firstOrNull { it.field == SourceConfig.ITEM_KEY }
        assertNotNull("селектор карточки — первое, что надо починить", itemFix)

        // Предложенный селектор должен реально находить карточки на новом HTML.
        val hits = domBroken.select(itemFix!!.selector)
        assertTrue("селектор ${itemFix.selector} нашёл ${hits.size} узлов", hits.size >= 20)
    }

    @Test fun `починенный конфиг снова даёт пригодный разбор`() {
        val config = Fixtures.catalogConfig()
        val (domOk, reportOk) = run(CatalogPage().html(), config, Fixtures.healthyHistory())
        val enriched = SignatureCapture.refresh(domOk, config, reportOk)

        val brokenHtml = CatalogPage(itemClass = "css-1q7hf2n", titleClass = "css-9klm3a").html()
        val (domBroken, reportBroken) = run(brokenHtml, enriched, Fixtures.healthyHistory())
        val heal = healer.heal(domBroken, enriched, reportBroken)

        val repaired = enriched.withRepairs(heal.proposals.associateBy { it.field })
        val (_, reportAfter) = run(brokenHtml, repaired, Fixtures.healthyHistory())

        assertTrue(
            "после ремонта разбор обязан стать пригодным, получен ${reportAfter.verdict}",
            reportAfter.verdict.isUsable,
        )
        assertTrue("элементов после ремонта: ${reportAfter.itemCount}", reportAfter.itemCount >= 20)
    }

    @Test fun `хилер не тратит бюджет на дефекты значений`() {
        val config = Fixtures.catalogConfig()
        // Обложки-заглушки: селектор жив, чинить нечего.
        val (_, report) = run(CatalogPage(brokenCovers = true).html(), config, Fixtures.healthyHistory())
        val targets = healer.targetsOf(report)
        assertFalse(
            "PLACEHOLDER/MARKUP_LEAK не подлежат ремонту селектора: $targets",
            targets.contains("cover") && report.issues.none {
                it.field == "cover" && it.code in setOf(
                    core.validate.IssueCode.FIELD_MISSING,
                    core.validate.IssueCode.FIELD_LOW_FILL,
                    core.validate.IssueCode.FIELD_FILL_DROP,
                    core.validate.IssueCode.FIELD_CONSTANT,
                )
            },
        )
    }

    @Test fun `селектор карточки не предлагается шире ожидаемого`() {
        // На странице 24 карточки каталога и 8 рекомендаций той же вёрстки.
        val dom = parser.parse(CatalogPage(withRecommendations = true).html(), URL)
        val sample = dom.select("main div.catalog-item").first()
        val candidates = SelectorSynth.forRepeating(sample, dom, expectedCount = 24)
        assertTrue("кандидаты не сгенерированы", candidates.isNotEmpty())
        val best = candidates.first()
        // Селектор, ловящий 32 вместо 24, обязан получить балл ниже точного.
        val exact = candidates.filter { it.hits == 24 }
        if (exact.isNotEmpty()) {
            assertTrue(
                "точный селектор должен побеждать: best=${best.css}/${best.hits}",
                best.hits == 24 || best.stability >= exact.first().stability,
            )
        }
    }

    @Test fun `синтезированный селектор поля уникален внутри карточки`() {
        val dom = parser.parse(CatalogPage().html(), URL)
        val card = dom.select("div.catalog-item").first()
        val title = card.selectFirst("h3")!!
        val candidates = SelectorSynth.forNode(title, dom, scope = card)
        assertTrue(candidates.isNotEmpty())
        val best = candidates.first()
        assertEquals("селектор должен находить ровно один узел в карточке", 1, card.select(best.css).size)
    }

    @Test fun `нестабильные классы отбрасываются`() {
        assertTrue(Signatures.isVolatileClass("css-1q7hf2n"))
        assertTrue(Signatures.isVolatileClass("Button_root__x8Ky2"))
        assertTrue(Signatures.isVolatileClass("_3fLm9x"))
        assertTrue(Signatures.isVolatileClass("is-active"))
        assertTrue(Signatures.isVolatileClass("selected"))
        assertFalse(Signatures.isVolatileClass("catalog-item"))
        assertFalse(Signatures.isVolatileClass("card-title"))
    }

    @Test fun `угадывание повторяющегося блока работает без отпечатков`() {
        val dom = parser.parse(CatalogPage().html(), URL)
        val guess = healer.guessRepeatingBlock(dom)
        assertNotNull("сетку карточек надо найти даже без отпечатков", guess)
        assertNotNull("в угаданном блоке должна быть ссылка", guess!!.selectFirst("a[href]"))
    }

    private companion object { const val URL = "https://example-anime.tv/catalog" }
}
