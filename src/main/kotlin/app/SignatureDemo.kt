package app

import core.heal.Signatures
import core.heal.confirm
import core.heal.Similarity
import core.heal.SignatureSearch
import core.model.ElementSignature
import core.parse.JsoupParser
import core.test.CatalogPage

private fun line(label: String, v: Double) =
    println("  ${label.padEnd(38)} ${"%.3f".format(v)}")

fun main() {
    val parser = JsoupParser()
    val base = CatalogPage().html()

    val dom = parser.parse(base, "https://example-anime.tv/catalog")
    val cards = dom.select("div.catalog-item")
    println("карточек на базовой странице: ${cards.size}")

    val card = cards.first()
    val sig = Signatures.capture(card, now = 1L)

    println("\n=== отпечаток карточки ===")
    println("  тег           ${sig.tag}")
    println("  классы        ${sig.stableClasses}")
    println("  форма         ${sig.shape}")
    println("  путь          ${sig.tagPath.takeLast(5).joinToString(">")}")
    println("  соседи        ${sig.neighbourTags}")
    println("  предки        ${sig.ancestorClasses}")
    println("  глубина       ${sig.depth}, позиция ${sig.indexInParent}")
    println("  токенов       ${sig.textTokens.size}")

    println("\n=== сходство ===")
    line("та же карточка сама с собой", Similarity.score(sig, card))
    line("соседняя карточка той же страницы", Similarity.score(sig, cards[1]))
    line("последняя карточка страницы", Similarity.score(sig, cards.last()))

    // редизайн: класс-контейнер заменён на сгенерированный хэш
    val redesign = base.replace("catalog-item", "css-1q7hf2n")
    val domR = parser.parse(redesign, "https://example-anime.tv/catalog")
    val cardR = domR.select("div.css-1q7hf2n").first()
    line("та же карточка после редизайна", Similarity.score(sig, cardR))

    dom.selectFirst("nav a")?.let { line("ссылка навигации", Similarity.score(sig, it)) }
    dom.selectFirst("h3")?.let { line("заголовок внутри карточки", Similarity.score(sig, it)) }

    val mixed = parser.parse(CatalogPage(withRecommendations = true).html(),
                             "https://example-anime.tv/catalog")
    mixed.selectFirst("div.rec-card")?.let { line("карточка рекомендаций", Similarity.score(sig, it)) }

    println("\n=== поиск после редизайна вслепую ===")
    val pool = SignatureSearch.pool(domR, sig)
    println("  кандидатов в пуле: ${pool.size}")
    val m = SignatureSearch.bestMatch(sig, pool)
    if (m == null) println("  не найдено") else {
        println("  балл ${"%.3f".format(m.score)}, второй ${"%.3f".format(m.runnerUp)}, отрыв ${"%.3f".format(m.margin)}")
        println("  уверенно: ${m.isConfident}")
        println("  найден: <${m.node.tag} class=\"${m.node.classes.joinToString(" ")}\">")
        println("  это первая карточка: ${m.node.indexInParent == card.indexInParent}")
    }

    println("\n=== подтверждение зрелости ===")
    var s: ElementSignature = sig
    repeat(4) { s = s.confirm(now = 2L) }
    println("  подтверждений ${s.confirmed}, зрелый: ${s.isMature}")
}
