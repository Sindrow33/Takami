package core.test

/**
 * Страницы терминального слоя с типовыми ловушками: баннеры вперемешку
 * со страницами главы, ленивые картинки, список в скрипте, комментарии
 * рядом с телом главы.
 */
object TerminalPages {

    /** Читалка манги: страницы в DOM, вокруг — шапка, баннер, «читайте также». */
    fun mangaDom(pages: Int = 18, lazyAttr: String = "data-src"): String = buildString {
        append("<!DOCTYPE html><html><head><title>Глава 84</title></head><body>")
        append("""<header><img src="/img/logo.png" width="120" height="40"></header>""")
        append("""<div class="ads"><img src="https://ads.example.net/banner-970x250.jpg" width="970" height="250"></div>""")
        append("""<div class="reader-container">""")
        for (i in 1..pages) {
            append("""<div class="page"><img class="page-img" src="/img/blank.gif" """)
            append("""$lazyAttr="https://cdn.example-manga.org/ch84/${i}.jpg" width="900" height="1300"></div>""")
        }
        append("</div>")
        append("""<aside class="also"><img src="/covers/other-1.jpg" width="100" height="140">""")
        append("""<img src="/covers/other-2.jpg" width="100" height="140"></aside>""")
        append("""<footer><img src="/img/icon-vk.png" width="24" height="24"></footer>""")
        append("</body></html>")
    }

    /** Читалка, где картинок в DOM нет: список страниц лежит в скрипте. */
    fun mangaScript(pages: Int = 12): String = buildString {
        append("<!DOCTYPE html><html><head><title>Глава 12</title></head><body>")
        append("""<div class="ads"><img src="https://ads.example.net/top.jpg"></div>""")
        append("<div id=\"reader\"></div>")
        append("<script>")
        append("var ads = [\"https://ads.example.net/side-1.jpg\",\"https://ads.example.net/side-2.jpg\"];")
        append("window.chapterPages = [")
        // Намеренно вперемешку по номерам: проверяем естественную сортировку.
        val order = (1..pages).shuffled(java.util.Random(7).let { r -> kotlin.random.Random(7) })
        append(order.joinToString(",") { "\"https:\\/\\/cdn.example-manga.org\\/vol1\\/ch12\\/page-$it.webp\"" })
        append("];")
        append("</script>")
        append("</body></html>")
    }

    /** Ранобэ: тело главы среди меню, комментариев и блока рекомендаций. */
    fun novel(paragraphs: Int = 14): String = buildString {
        append("<!DOCTYPE html><html><head><title>Тихий дом на холме — Глава 12</title></head><body>")
        append("""<nav><a href="/">Главная</a><a href="/catalog">Каталог</a><a href="/top">Топ</a></nav>""")
        append("<h1>Глава 12. Дождь над крышей</h1>")
        append("""<div class="text-container"><div class="chapter-text">""")
        for (i in 1..paragraphs) {
            append("<p>")
            append("Дождь начался задолго до рассвета, и к утру черепица потемнела так, ")
            append("что казалась почти чёрной. Абзац номер $i продолжает эту мысль ровно ")
            append("настолько, насколько нужно, чтобы плотность текста была похожа на прозу, ")
            append("а не на список ссылок или подпись под картинкой.")
            append("</p>")
        }
        append("</div></div>")
        append("""<div class="share">Поделиться: <a href="#">ВК</a> <a href="#">Телеграм</a></div>""")
        append("""<div class="comments"><p>Первый!</p><p>Спасибо за перевод</p></div>""")
        append("""<aside class="related"><a href="/n/1">Другая новелла</a><a href="/n/2">И ещё одна</a>""")
        append("""<a href="/n/3">И третья</a><a href="/n/4">И четвёртая</a></aside>""")
        append("</body></html>")
    }

    /** Ранобэ на старом движке: абзацы разделены <br><br>, тегов <p> нет. */
    fun novelWithBr(paragraphs: Int = 10): String = buildString {
        append("<!DOCTYPE html><html><head><title>Глава 3</title></head><body>")
        append("<h1>Глава 3</h1><div id=\"content\">")
        for (i in 1..paragraphs) {
            append("Абзац $i. Ветер шёл по склону вниз, задевая верхушки сосен, и в этом ")
            append("движении не было ничего тревожного — так бывает каждую осень, когда ")
            append("тепло уходит окончательно и воздух делается прозрачным.")
            append("<br><br>")
        }
        append("</div></body></html>")
    }

    /** Плеер: ссылка на HLS-манифест в inline-скрипте инициализации. */
    fun animeInline(): String = """
        <!DOCTYPE html><html><head><title>Эпизод 8</title></head><body>
        <div id="player"></div>
        <script>
          var poster = "/img/poster-1080.jpg";
          new Player({
            "480p": "https://cdn.example-anime.tv/s1/e8/480/index.m3u8",
            "720p": "https://cdn.example-anime.tv/s1/e8/720/index.m3u8",
            "1080p": "https://cdn.example-anime.tv/s1/e8/1080/index.m3u8"
          });
        </script>
        </body></html>
    """.trimIndent()

    /** Плеер во внешнем фрейме: потока в HTML нет и быть не может. */
    fun animeFrame(): String = """
        <!DOCTYPE html><html><head><title>Эпизод 9</title></head><body>
        <div class="player-box">
          <iframe src="https://kodik.example/serial/123/abc/720p" allowfullscreen></iframe>
        </div>
        </body></html>
    """.trimIndent()

    /** Прямой <video> с несколькими source. */
    fun animeDirect(): String = """
        <!DOCTYPE html><html><body>
        <video controls>
          <source src="/media/e10-480.mp4" label="480p">
          <source src="/media/e10-1080.mp4" label="1080p">
        </video>
        </body></html>
    """.trimIndent()
}
