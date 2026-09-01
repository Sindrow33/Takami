package core

import core.model.ContentProfile
import core.model.MediaKind
import core.model.SourceConfig
import core.model.StreamKind
import core.model.TerminalContent
import core.model.TerminalExpect
import core.model.TerminalSpec
import core.parse.JsoupParser
import core.terminal.TerminalExtractor
import core.test.TerminalPages
import org.junit.Assert.*
import org.junit.Test

class TerminalTest {

    private val parser = JsoupParser()
    private val extractor = TerminalExtractor()

    private fun config(kind: MediaKind, spec: TerminalSpec? = null) = SourceConfig(
        host = "example.org",
        profile = ContentProfile.of(kind),
        terminal = spec,
    )

    /* ---------------- манга ---------------- */

    @Test fun `страницы главы находятся среди баннеров и превью`() {
        val dom = parser.parse(TerminalPages.mangaDom(pages = 18), URL_MANGA)
        val c = extractor.extract(dom, config(MediaKind.MANGA), URL_MANGA)

        assertTrue("получено ${c::class.simpleName}", c is TerminalContent.Images)
        val pages = (c as TerminalContent.Images).pages
        assertEquals("должны остаться только страницы главы", 18, pages.size)
        assertTrue("в выдачу попала реклама: ${pages.map { it.url }}",
            pages.none { it.url.contains("ads.example.net") })
        assertTrue("в выдачу попали превью других тайтлов",
            pages.none { it.url.contains("/covers/") })
        assertTrue("в выдачу попали иконки",
            pages.none { it.url.contains("logo") || it.url.contains("icon") })
    }

    @Test fun `ленивые картинки читаются из data-атрибута, а не из заглушки`() {
        val dom = parser.parse(TerminalPages.mangaDom(pages = 6), URL_MANGA)
        val c = extractor.extract(dom, config(MediaKind.MANGA), URL_MANGA) as TerminalContent.Images
        assertTrue("вместо страниц взяты заглушки: ${c.pages.first().url}",
            c.pages.all { it.url.contains("cdn.example-manga.org") })
        assertTrue(c.pages.none { it.url.contains("blank.gif") })
    }

    @Test fun `нестандартный lazy-атрибут тоже поддерживается`() {
        val dom = parser.parse(TerminalPages.mangaDom(pages = 5, lazyAttr = "data-original"), URL_MANGA)
        val c = extractor.extract(dom, config(MediaKind.MANGA), URL_MANGA) as TerminalContent.Images
        assertEquals(5, c.pages.size)
        assertTrue(c.pages.all { it.url.contains("cdn.example-manga.org") })
    }

    @Test fun `список страниц вытаскивается из скрипта, когда в DOM их нет`() {
        val dom = parser.parse(TerminalPages.mangaScript(pages = 12), URL_MANGA)
        val c = extractor.extract(dom, config(MediaKind.MANGA), URL_MANGA)

        assertTrue("получено ${c::class.simpleName}", c is TerminalContent.Images)
        val pages = (c as TerminalContent.Images).pages
        assertEquals(12, pages.size)
        assertTrue("реклама из соседнего массива попала в главу",
            pages.none { it.url.contains("ads.example.net") })
    }

    @Test fun `страницы из скрипта идут в естественном порядке`() {
        val dom = parser.parse(TerminalPages.mangaScript(pages = 12), URL_MANGA)
        val c = extractor.extract(dom, config(MediaKind.MANGA), URL_MANGA) as TerminalContent.Images

        val numbers = c.pages.map { it.url.substringAfterLast("page-").substringBefore('.').toInt() }
        assertEquals("page-10 не должна идти перед page-2", (1..12).toList(), numbers)
    }

    @Test fun `явный селектор из конфига имеет приоритет`() {
        val dom = parser.parse(TerminalPages.mangaDom(pages = 9), URL_MANGA)
        val spec = TerminalSpec(
            expect = TerminalExpect.IMAGES,
            imageArraySelectors = listOf("div.reader-container img.page-img"),
            requiredHeaders = mapOf("Referer" to "https://example-manga.org/"),
        )
        val c = extractor.extract(dom, config(MediaKind.MANGA, spec), URL_MANGA) as TerminalContent.Images
        assertEquals(9, c.pages.size)
        assertEquals("заголовки из конфига обязаны дойти до загрузчика",
            "https://example-manga.org/", c.pages.first().headers["Referer"])
    }

    @Test fun `пустая страница честно сообщает о неудаче`() {
        val dom = parser.parse("<html><body><p>Глава удалена</p></body></html>", URL_MANGA)
        val c = extractor.extract(dom, config(MediaKind.MANGA), URL_MANGA)
        assertTrue(c is TerminalContent.Unavailable)
        assertTrue(c.isEmpty)
    }

    /* ---------------- ранобэ ---------------- */

    @Test fun `тело главы отделяется от меню и комментариев`() {
        val dom = parser.parse(TerminalPages.novel(paragraphs = 14), URL_NOVEL)
        val c = extractor.extract(dom, config(MediaKind.NOVEL), URL_NOVEL)

        assertTrue("получено ${c::class.simpleName}", c is TerminalContent.Text)
        val ch = (c as TerminalContent.Text).chapter
        assertEquals("должны остаться только абзацы главы", 14, ch.paragraphs.size)
        assertTrue("в тело попали комментарии", ch.paragraphs.none { it.contains("Спасибо за перевод") })
        assertTrue("в тело попал блок «поделиться»", ch.paragraphs.none { it.contains("Поделиться") })
        assertTrue(ch.charCount > 400)
    }

    @Test fun `заголовок главы берётся из h1`() {
        val dom = parser.parse(TerminalPages.novel(), URL_NOVEL)
        val c = extractor.extract(dom, config(MediaKind.NOVEL), URL_NOVEL) as TerminalContent.Text
        assertEquals("Глава 12. Дождь над крышей", c.chapter.title)
    }

    @Test fun `старый движок с br вместо p тоже читается`() {
        val dom = parser.parse(TerminalPages.novelWithBr(paragraphs = 10), URL_NOVEL)
        val c = extractor.extract(dom, config(MediaKind.NOVEL), URL_NOVEL)

        assertTrue("получено ${c::class.simpleName}", c is TerminalContent.Text)
        val ch = (c as TerminalContent.Text).chapter
        assertTrue("абзацев распознано ${ch.paragraphs.size}", ch.paragraphs.size >= 8)
    }

    @Test fun `страница без прозы не выдаётся за главу`() {
        val nav = "<html><body><nav>" +
            (1..40).joinToString("") { "<a href=\"/c/$it\">Глава $it</a>" } +
            "</nav></body></html>"
        val dom = parser.parse(nav, URL_NOVEL)
        val c = extractor.extract(dom, config(MediaKind.NOVEL), URL_NOVEL)
        assertTrue("список ссылок принят за тело главы", c is TerminalContent.Unavailable)
    }

    /* ---------------- аниме ---------------- */

    @Test fun `hls-манифесты вытаскиваются из инициализации плеера`() {
        val dom = parser.parse(TerminalPages.animeInline(), URL_ANIME)
        val c = extractor.extract(dom, config(MediaKind.VIDEO), URL_ANIME)

        assertTrue("получено ${c::class.simpleName}", c is TerminalContent.Streams)
        val streams = (c as TerminalContent.Streams).streams
        assertEquals(3, streams.size)
        assertTrue(streams.all { it.kind == StreamKind.HLS })
        assertEquals("лучшее качество должно быть первым", 1080, streams.first().height)
        assertTrue("постер не поток", streams.none { it.url.contains("poster") })
    }

    @Test fun `поток получает Referer и Origin`() {
        val dom = parser.parse(TerminalPages.animeInline(), URL_ANIME)
        val c = extractor.extract(dom, config(MediaKind.VIDEO), URL_ANIME) as TerminalContent.Streams
        val s = c.streams.first()
        assertEquals(URL_ANIME, s.headers["Referer"])
        assertEquals("https://example-anime.tv", s.headers["Origin"])
    }

    @Test fun `прямой video с несколькими source разбирается`() {
        val dom = parser.parse(TerminalPages.animeDirect(), URL_ANIME)
        val c = extractor.extract(dom, config(MediaKind.VIDEO), URL_ANIME) as TerminalContent.Streams
        assertEquals(2, c.streams.size)
        assertTrue(c.streams.all { it.kind == StreamKind.PROGRESSIVE })
        assertTrue(c.streams.all { it.url.startsWith("https://example-anime.tv/media/") })
    }

    @Test fun `плеер во фрейме сообщает адрес, а не молчит`() {
        val dom = parser.parse(TerminalPages.animeFrame(), URL_ANIME)
        val c = extractor.extract(dom, config(MediaKind.VIDEO), URL_ANIME)

        assertTrue(c is TerminalContent.Unavailable)
        val msg = (c as TerminalContent.Unavailable).message
        assertTrue("сообщение должно вести к фрейму: $msg", msg.contains("kodik.example"))
    }

    /* ---------------- выбор резолвера ---------------- */

    @Test fun `тип контента выводится из профиля, когда конфиг молчит`() {
        val dom = parser.parse(TerminalPages.mangaDom(pages = 7), URL_MANGA)
        assertTrue(extractor.extract(dom, config(MediaKind.MANGA), URL_MANGA) is TerminalContent.Images)

        val novelDom = parser.parse(TerminalPages.novel(), URL_NOVEL)
        assertTrue(extractor.extract(novelDom, config(MediaKind.NOVEL), URL_NOVEL) is TerminalContent.Text)

        val animeDom = parser.parse(TerminalPages.animeInline(), URL_ANIME)
        assertTrue(extractor.extract(animeDom, config(MediaKind.VIDEO), URL_ANIME) is TerminalContent.Streams)
    }

    private companion object {
        const val URL_MANGA = "https://example-manga.org/ch/84"
        const val URL_NOVEL = "https://example-novel.org/n/1/12"
        const val URL_ANIME = "https://example-anime.tv/watch/8"
    }
}
