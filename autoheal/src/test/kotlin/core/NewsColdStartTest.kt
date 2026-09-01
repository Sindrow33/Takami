package core

import core.extract.RequestKind
import core.extract.StandardExtractor
import core.heal.ColdStart
import core.model.ContentProfile
import core.model.MediaKind
import core.model.ParsedPayload
import core.model.SourceConfig
import core.parse.JsoupParser
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Новостная лента разбирается тем же холодным стартом, что и каталог.
 *
 * Профиль новостей отличается контрактом, а не разметкой: у новости нет
 * глав, зато важна дата. Проверяется на реальной странице Anime News
 * Network — вымышленная разметка тут ничего не доказала бы, весь смысл
 * в том, что живые ленты свёрстаны не так, как удобно парсеру.
 */
class NewsColdStartTest {

    private val url = "https://www.animenewsnetwork.com/news/"

    private fun dom() = JsoupParser().parse(
        checkNotNull(javaClass.classLoader.getResourceAsStream("ann-news.html")).bufferedReader().readText(),
        url,
    )

    private fun newsConfig() = SourceConfig(
        host = "animenewsnetwork.com",
        profile = ContentProfile.of(MediaKind.NEWS),
    )

    @Test
    fun newsListingIsInferred() {
        val inferred = ColdStart.infer(dom(), newsConfig())
        assertNotNull("конфиг новостной ленты не сочинён", inferred)
        val fields = inferred!!.listing!!.fields
        assertTrue("нет заголовка", fields.containsKey("title"))
        assertTrue("нет ссылки", fields.containsKey("url"))
    }

    @Test
    fun newsItemsAreExtracted() {
        val dom = dom()
        val inferred = ColdStart.infer(dom, newsConfig())!!
        val items = (
            StandardExtractor().extract(RequestKind.LISTING, dom, inferred, url).payload
                as ParsedPayload.Listing
            ).items

        assertTrue("нашли всего ${items.size} новостей", items.size >= 10)
        assertTrue("пустые заголовки", items.all { it.title.isNotBlank() })
        assertTrue("пустые ссылки", items.all { it.url.isNotBlank() })
    }

    @Test
    fun newsProfileExpectsNoUnits() {
        // У новости нет глав и серий. Если профиль потребует хотя бы
        // одну единицу, валидатор забракует корректно разобранную ленту.
        val profile = ContentProfile.of(MediaKind.NEWS)
        assertTrue("профиль новостей требует единиц", profile.unitsMin == 0)
    }
}
