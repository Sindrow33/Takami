package dev.takami.app

import dev.takami.app.home.NewsFeed
import dev.takami.app.news.DiscoveredNews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsFeedTest {

    private fun item(key: String, date: String = "", host: String = "www.animenewsnetwork.com") =
        DiscoveredNews.Item(
            key = key,
            title = "Заголовок $key",
            url = "https://$host/news/$key",
            date = date,
            host = host,
        )

    private fun at(iso: String): Long = NewsFeed.parseDate(iso)!!

    @Test
    fun `новость без даты не получает выдуманный возраст`() {
        val cards = NewsFeed.cards(listOf(item("a")), now = at("2026-09-01"))
        assertEquals("возраст без даты — это утверждение, которого никто не проверял", "", cards[0].age)
    }

    @Test
    fun `возраст считается словами`() {
        val now = at("2026-09-01")
        assertEquals("сегодня", NewsFeed.age("2026-09-01", now))
        assertEquals("вчера", NewsFeed.age("2026-08-31", now))
        assertEquals("3 дн. назад", NewsFeed.age("2026-08-29", now))
        assertEquals("2 нед. назад", NewsFeed.age("2026-08-16", now))
        assertEquals("2 мес. назад", NewsFeed.age("2026-07-01", now))
    }

    @Test
    fun `дата из будущего не превращается в отрицательный возраст`() {
        assertEquals("", NewsFeed.age("2026-09-05", at("2026-09-01")))
    }

    @Test
    fun `дата снимается из полной метки времени`() {
        assertEquals(
            "полная метка времени — тот же день, что и голая дата",
            at("2026-08-30"),
            NewsFeed.parseDate("2026-08-30T14:22:00+09:00")!!.toLong(),
        )
    }

    @Test
    fun `мусор вместо даты не разбирается`() {
        assertNull(NewsFeed.parseDate(""))
        assertNull(NewsFeed.parseDate("вчера"))
        assertNull("месяц 13 — не дата, а совпавший узор", NewsFeed.parseDate("2026-13-02"))
    }

    @Test
    fun `карусель обрезается до предела`() {
        val many = (1..40).map { item("k$it") }
        assertEquals(NewsFeed.LIMIT, NewsFeed.cards(many).size)
    }

    @Test
    fun `подпись карточки — источник и адрес, без выдуманной рубрики`() {
        val card = NewsFeed.cards(listOf(item("k1")))[0]
        assertEquals("animenewsnetwork.com", card.source)
        assertEquals("news/k1", card.subtitle)
        assertTrue("без адреса карточка не открывается", card.url.startsWith("https://"))
    }

    @Test
    fun `пустая лента не даёт карточек`() {
        assertTrue(NewsFeed.cards(emptyList()).isEmpty())
    }
}
