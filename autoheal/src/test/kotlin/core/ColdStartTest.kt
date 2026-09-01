package core

import core.engine.ParseEngine
import core.extract.RequestKind
import core.heal.ColdStart
import core.model.ContentProfile
import core.model.MediaKind
import core.model.ParsedPayload
import core.model.SourceConfig
import core.parse.JsoupParser
import core.store.SourceRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Холодный старт: сайт, который движок видит впервые.
 *
 * Проверяется на РЕАЛЬНОЙ разметке AnimeVost, сохранённой в ресурсах, а
 * не на синтетическом HTML: весь смысл этих правок в том, что на живом
 * сайте разбор возвращал ноль элементов, а сообщение об ошибке
 * сваливало это на «страницу собирают скрипты», хотя каталог лежит в
 * статическом HTML целиком.
 */
class ColdStartTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun html(name: String): String =
        checkNotNull(javaClass.classLoader.getResourceAsStream(name)) { "нет фикстуры $name" }
            .bufferedReader().readText()

    private val url = "https://animevost.one/"

    private fun bundled() = SourceConfig(
        host = "animevost.one",
        profile = ContentProfile.of(MediaKind.VIDEO),
    )

    @Test
    fun `сочиняет listing для незнакомого сайта`() {
        val dom = JsoupParser().parse(html("animevost-listing.html"), url)
        val inferred = ColdStart.infer(dom, bundled())

        assertNotNull("конфиг не сочинён — каталог остался невидимым", inferred)
        val listing = inferred!!.listing!!
        assertTrue("нет поля title", listing.fields.containsKey("title"))
        assertTrue("нет поля url", listing.fields.containsKey("url"))
    }

    @Test
    fun `движок находит тайтлы там, где раньше был пустой список`() {
        val registry = SourceRegistry(tmp.newFolder())
        val engine = ParseEngine(registry)

        val result = engine.parse(
            host = "animevost.one",
            html = html("animevost-listing.html"),
            url = url,
            bundled = bundled(),
            kind = RequestKind.LISTING,
        )

        val items = (result.payload as ParsedPayload.Listing).items
        assertTrue("нашли всего ${items.size} тайтлов", items.size >= 5)
        assertTrue("у тайтлов пустые заголовки", items.all { it.title.isNotBlank() })
        assertTrue("у тайтлов пустые адреса", items.all { it.url.isNotBlank() })
    }

    @Test
    fun `навигация по жанрам не выдаётся за каталог`() {
        val dom = JsoupParser().parse(html("animevost-listing.html"), url)
        val inferred = ColdStart.infer(dom, bundled())!!
        val items = (
            core.extract.StandardExtractor()
                .extract(RequestKind.LISTING, dom, inferred, url).payload as ParsedPayload.Listing
            ).items

        // Меню жанров — это тоже десятки однотипных ссылок подряд;
        // именно на нём наивный синтез селектора и промахивался.
        assertTrue(
            "в каталог просочилась навигация: ${items.take(3).map { it.url }}",
            items.none { "/genres/" in it.url || "/year/" in it.url },
        )
    }

    @Test
    fun `готовый конфиг не переписывается`() {
        val dom = JsoupParser().parse(html("animevost-listing.html"), url)
        val ready = ColdStart.infer(dom, bundled())!!
        assertNull("сочинение поверх готового конфига", ColdStart.infer(dom, ready))
    }

    @Test
    fun `на странице без повторяющихся блоков сочинять нечего`() {
        val dom = JsoupParser().parse(
            "<html><body><h1>Заголовок</h1><p>Просто текст</p></body></html>",
            url,
        )
        assertNull(ColdStart.infer(dom, bundled()))
    }

    @Test
    fun `ссылки навигации отсеиваются на уровне карточки`() {
        val dom = JsoupParser().parse(
            """
            <html><body><article class="post">
              <h2>Название тайтла</h2>
              <div><a href="/genres/action/">Экшен</a>
                   <a href="/type/tv/8376-dark-moon.html">Смотреть</a></div>
            </article></body></html>
            """.trimIndent(),
            url,
        )
        val card = dom.selectFirst("article.post")!!
        assertEquals("https://animevost.one/type/tv/8376-dark-moon.html", ColdStart.contentHrefOf(card))
    }
}
