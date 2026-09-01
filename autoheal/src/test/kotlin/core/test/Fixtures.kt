package core.test

import core.extract.RequestKind
import core.extract.StandardExtractor
import core.model.*
import core.parse.JsoupParser
import core.validate.*

/* =====================================================================
 * 1. ГЕНЕРАТОР СТРАНИЦ
 * ===================================================================== */

/**
 * Строит HTML каталога с управляемыми дефектами. Смысл именно в
 * параметризации: одна и та же страница в двух состояниях — до и после
 * поломки — позволяет проверить, что система видит разницу.
 */
class CatalogPage(
    private val itemClass: String = "catalog-item",
    private val titleClass: String = "card-title",
    private val count: Int = 24,
    private val coverAttr: String = "data-src",
    private val withNav: Boolean = true,
    private val withRecommendations: Boolean = false,
    private val brokenCovers: Boolean = false,
    private val duplicateUrls: Boolean = false,
    private val withMicrodata: Boolean = false,
) {
    fun html(): String = buildString {
        append("<!DOCTYPE html><html><head><title>Каталог</title>")
        append("<meta property=\"og:title\" content=\"Каталог аниме\">")
        append("</head><body>")

        if (withNav) append(navBlock())
        if (withRecommendations) append(recommendationsBlock())

        append("<main><div class=\"grid\">")
        for (i in 1..count) append(card(i))
        append("</div>")
        append("<a class=\"pager-next\" href=\"/catalog?page=2\">Далее</a>")
        append("</main>")
        append(footerBlock())
        append("</body></html>")
    }

    private fun card(i: Int): String {
        val slug = TITLES[(i - 1) % TITLES.size].second
        val title = TITLES[(i - 1) % TITLES.size].first
        val url = if (duplicateUrls) "/anime/same-title" else "/anime/$slug-$i"
        val cover = if (brokenCovers) "data:image/gif;base64,R0lGOD" else "/img/$slug-$i.jpg"
        val micro = if (withMicrodata) " itemscope itemtype=\"https://schema.org/CreativeWork\"" else ""
        val microName = if (withMicrodata) " itemprop=\"name\"" else ""

        return """
        <div class="$itemClass"$micro>
          <a href="$url" class="card-link">
            <img class="card-poster" $coverAttr="$cover" src="/img/blank.gif" alt="">
          </a>
          <h3 class="$titleClass"$microName>$title</h3>
          <span class="card-year">${2019 + (i % 6)}</span>
          <span class="card-eps">${(i % 24) + 1} серий</span>
        </div>""".trimIndent()
    }

    /** Хлебные крошки — типовая ловушка для фолбэков вида ".list > div". */
    private fun navBlock(): String = """
        <header><nav class="list">
          <div><a href="/">Главная</a></div>
          <div><a href="/catalog">Каталог</a></div>
          <div><a href="/random">Случайное</a></div>
        </nav></header>""".trimIndent()

    /**
     * Главная ловушка сценария Б: карточки той же вёрстки, но не каталог.
     * Их ровно восемь, они одинаковы на всех страницах.
     */
    private fun recommendationsBlock(): String = buildString {
        append("<aside class=\"recommend\"><h2>Вам понравится</h2><div class=\"grid\">")
        for (i in 1..8) {
            append("""
            <div class="$itemClass">
              <a href="/anime/rec-$i" class="card-link">
                <img class="card-poster" $coverAttr="/img/rec-$i.jpg" src="/img/blank.gif">
              </a>
              <h3 class="$titleClass">${REC_TITLES[i - 1]}</h3>
            </div>""".trimIndent())
        }
        append("</div></aside>")
    }

    private fun footerBlock(): String =
        """<footer><div class="list"><div>О сайте</div><div>Контакты</div></div></footer>"""

    companion object {
        val TITLES = listOf(
            "Магическая битва" to "jujutsu",
            "Клинок, рассекающий демонов" to "kimetsu",
            "Атака титанов" to "shingeki",
            "Ван Пис" to "onepiece",
            "Стальной алхимик" to "fma",
            "Тетрадь смерти" to "deathnote",
        )
        val REC_TITLES = listOf(
            "Наруто", "Блич", "Хантер х Хантер", "Токийский гуль",
            "Обещанный Неверленд", "Доктор Стоун", "Ре:Зеро", "Мастера меча",
        )
    }
}

/* =====================================================================
 * 2. КОНФИГИ
 * ===================================================================== */

object Fixtures {

    fun catalogConfig(itemSelector: String = "div.catalog-item"): SourceConfig = SourceConfig(
        host = "example-anime.tv",
        profile = ContentProfile.VIDEO,
        listing = ListingSpec(
            itemSelector = itemSelector,
            fallbackItemSelectors = listOf("article.item", ".list > div", "[itemtype*=CreativeWork]"),
            nextPageSelector = "a.pager-next",
            fields = mapOf(
                "title" to FieldSpec(
                    selector = ".card-title",
                    fallbackSelectors = listOf("h3", "a[title]"),
                    required = true,
                    goldenValue = "Магическая битва",
                ),
                "url" to FieldSpec(
                    selector = "a.card-link",
                    fallbackSelectors = listOf("a[href]"),
                    transform = ValueTransform.HREF,
                    required = true,
                ),
                "cover" to FieldSpec(
                    selector = "img.card-poster",
                    fallbackSelectors = listOf("img"),
                    transform = ValueTransform.SRC,
                ),
                "year" to FieldSpec(selector = ".card-year"),
                "episodes" to FieldSpec(
                    selector = ".card-eps",
                    transform = ValueTransform(regex = """(\d+)"""),
                ),
            ),
        ),
        canaryUrls = listOf("https://example-anime.tv/catalog"),
    )

    /** Здоровье источника, проработавшего четыре месяца без сбоев. */
    fun healthyHistory(): SourceHealth = SourceHealth(
        host = "example-anime.tv",
        lastGoodCount = 24,
        emaFill = 0.97,
        emaConfidence = 0.94,
        stableFields = setOf("title", "url", "cover", "year", "episodes"),
        fieldFill = mapOf(
            "title" to 1.00, "url" to 1.00, "cover" to 0.99,
            "year" to 0.97, "episodes" to 0.96,
        ),
        successCount = 340,
        failureCount = 3,
        avgBodySize = 148_320,
    )

    /** Новый источник без истории — важен для проверки EmptyByDesign. */
    fun freshHistory(): SourceHealth = SourceHealth(host = "unknown.tv")
}

/* =====================================================================
 * 3. ПРОГОН
 * ===================================================================== */

data class RunOutcome(
    val payload: ParsedPayload,
    val report: ContractReport,
    val itemCount: Int,
) {
    fun describe(): String = buildString {
        appendLine("вердикт: ${report.verdict}")
        appendLine("элементов: $itemCount")
        appendLine("заполненность: " + report.fillRates.entries
            .sortedBy { it.key }
            .joinToString(", ") { "${it.key}=${"%.2f".format(it.value)}" })
        if (report.issues.isEmpty()) appendLine("проблем нет")
        else report.issues.sortedByDescending { it.severity.ordinal }.forEach {
            appendLine("  [${it.severity}] ${it.code}${it.field?.let { f -> " ($f)" } ?: ""}: ${it.message}")
        }
        report.trace?.let {
            if (it.usedFallback) appendLine("  фолбэки: item=${it.itemRung}, поля=${it.fieldRungs.filterValues { r -> r > 0 }}")
            if (it.droppedItems > 0) appendLine("  отброшено: ${it.droppedItems}")
        }
    }
}

class Harness(
    private val parser: JsoupParser = JsoupParser(),
    private val extractor: StandardExtractor = StandardExtractor(),
) {
    fun run(
        html: String,
        config: SourceConfig,
        health: SourceHealth?,
        url: String = "https://example-anime.tv/catalog",
        kind: RequestKind = RequestKind.LISTING,
    ): RunOutcome {
        val dom = parser.parse(html, url)
        val extracted = extractor.extract(kind, dom, config, url)
        val checker = StandardContractChecker(config.profile)
        val report = checker.check(extracted.payload, extracted.trace, health)
        return RunOutcome(extracted.payload, report, extracted.payload.size)
    }
}

/* =====================================================================
 * 4. ПРОГОН ЧЕРЕЗ ДВИЖОК (с реестром, ремонтом и ревизиями)
 * ===================================================================== */

/**
 * Полный контур на временном каталоге: то же, что в бою, только
 * состояние живёт в tmp и умирает вместе с тестом.
 */
class EngineHarness(root: java.io.File) {
    val registry = core.store.SourceRegistry(root)
    private val engine = core.engine.ParseEngine(registry)

    fun run(
        html: String,
        config: SourceConfig,
        host: String = config.host,
        url: String = "https://example-anime.tv/catalog",
        kind: core.extract.RequestKind = core.extract.RequestKind.LISTING,
    ) = engine.parse(host, html, url, config, kind)
}
