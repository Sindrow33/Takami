package app

import core.heal.*
import core.parse.JsoupParser
import core.test.CatalogPage
import java.io.File

fun main() {
    val p = JsoupParser()
    val base = p.parse(CatalogPage().html(), "https://example-anime.tv/catalog")
    val redesign = p.parse(CatalogPage(itemClass = "css-1q7hf2n", titleClass = "css-9klm3a").html(),
        "https://example-anime.tv/catalog")
    val recs = p.parse(CatalogPage(withRecommendations = true).html(), "https://example-anime.tv/catalog")

    val card = base.select("div.catalog-item").first()
    val title = card.select(".card-title").first()
    val sigCard = captureSignature(card)
    val sigTitle = captureSignature(title)

    println("подпись карточки: tag=${sigCard.tag} classes=${sigCard.stableClasses} " +
            "path=${sigCard.tagPath.takeLast(4)} shape=${sigCard.shape}")
    println("подпись заголовка: tokens=${sigTitle.textTokens.take(4)} depth=${sigTitle.depth}\n")

    fun probe(label: String, css: String, target: core.model.ElementSignature, dom: core.parse.Dom) {
        val m = bestMatch(target, dom.select(css), minScore = 0.0)
        println("%-30s кандидатов=%-4d лучший=%.2f  %s".format(
            label, dom.select(css).size, m?.second ?: 0.0,
            when {
                m == null -> "нет"
                m.second >= SIG_STRONG -> "сильное совпадение"
                m.second >= SIG_ACCEPT -> "принимаемо"
                else -> "ниже порога"
            }))
    }

    probe("карточка в себе", "div.catalog-item", sigCard, base)
    probe("карточка после редизайна", "div", sigCard, redesign)
    probe("карточка среди article", "article", sigCard, redesign)
    probe("заголовок после редизайна", "h3, .css-9klm3a", sigTitle, redesign)
    probe("карточка vs рекомендация", "div.rec-card", sigCard, recs)
    probe("карточка vs навигация", "nav a, .breadcrumbs a", sigCard, base)

    val root = File(System.getProperty("java.io.tmpdir"), "autoheal-sig")
    root.deleteRecursively()
    val store = SignatureStore(File(root, "sig"))
    repeat(3) { store.remember("example-anime.tv", "__item", captureSignature(card)) }
    val kept = store.book("example-anime.tv").byField["__item"]!!
    println("\nподтверждений после трёх встреч: ${kept.confirmed}, зрелая=${kept.isMature}")
}
