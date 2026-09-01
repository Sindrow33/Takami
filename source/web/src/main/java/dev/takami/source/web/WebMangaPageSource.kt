package dev.takami.source.web

import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageCacheKey
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import core.engine.ParseEngine
import core.extract.RequestKind
import core.model.ParsedPayload
import core.model.SourceConfig
import core.model.TerminalContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Источник страниц поверх автопарсера.
 *
 * Отвечает ровно за три вещи: получить HTML главы, вытащить из него
 * список страниц через `ParseEngine`, и отдать читалке файл на диске.
 * Разбор разметки, ремонт селекторов и вежливость к хосту — не здесь,
 * они уже есть в автопарсере.
 *
 * Дисковый кеш страниц и упреждающая загрузка живут в движке читалки:
 * этот слой сознательно не заводит второго слоя кеша байт, иначе одна
 * страница лежала бы на устройстве дважды.
 */
class WebMangaPageSource(
    private val engine: ParseEngine,
    private val fetcher: PageFetcher,
    private val bundledConfig: SourceConfig,
    /**
     * Каталог для скачанных страниц. Обычно подкаталог `cacheDir`, взятый
     * дисковым кешем читалки под общий лимит через `adopt` — значит,
     * файлы отсюда вытесняются по LRU и могут исчезнуть между вызовами.
     */
    private val downloadDir: File,
    /** URL главы по её id. Задаёт хост — у него список глав, не у нас. */
    private val chapterUrlOf: suspend (String) -> String,
    /** Полный список id глав тайтла в порядке чтения, для соседей. */
    private val chapterIdsOf: suspend (String) -> List<String>,
    /**
     * Сохранение отремонтированного конфига. Не опционально: если
     * ремонт нашёл рабочие селекторы и их не записать, следующая глава
     * снова пойдёт по сломанным и будет лечиться с нуля.
     */
    private val persistConfig: (SourceConfig) -> Unit,
) : MangaPageSource {

    /*
     * Разобранная глава кешируется в памяти: `pages()` читалка зовёт и
     * при открытии, и при повороте экрана, а каждый вызов иначе значил
     * бы новый сетевой запрос и новый прогон ремонта селекторов.
     */
    private val plans = LinkedHashMap<String, List<PageRef>>()
    private val plansMutex = Mutex()

    override suspend fun pages(chapterId: String): List<PageRef> {
        plansMutex.withLock { plans[chapterId] }?.let { return it }

        val chapterUrl = chapterUrlOf(chapterId)
        val host = ChapterPlan.originOf(chapterUrl)
            ?.substringAfter("://")
            ?: throw PageSourceException("глава без пригодного URL: $chapterUrl")

        val html = fetcher.html(chapterUrl)
        val result = engine.parse(
            host = host,
            html = html,
            url = chapterUrl,
            bundled = bundledConfig,
            kind = RequestKind.CONTENT,
        )

        result.configToPersist?.let(persistConfig)

        if (!result.isUsable) {
            throw PageSourceException("страницы главы не разобраны: ${result.report.verdict}")
        }

        val content = (result.payload as? ParsedPayload.Content)?.content
            ?: throw PageSourceException("парсер вернул не содержимое главы")
        val images = (content as? TerminalContent.Images)?.pages
            ?: throw PageSourceException("в главе нет изображений: $content")

        val refs = ChapterPlan.toPageRefs(chapterId, chapterUrl, images)
        if (refs.isEmpty()) throw PageSourceException("глава без страниц")

        plansMutex.withLock { plans[chapterId] = refs }
        return refs
    }

    override fun open(page: PageRef): Flow<PageLoad> = channelFlow {
        val target = File(downloadDir, fileNameFor(page))

        /*
         * Готовый файл переиспользуется. Проверка на непустоту не
         * лишняя: нулевой файл остаётся после падения между созданием
         * и записью, и читалка приняла бы его за готовую страницу.
         */
        if (target.isFile && target.length() > 0) {
            /*
             * Отметка времени обязательна: каталог вытесняется по LRU
             * от `lastModified`, а переиспользование файла его не
             * меняет. Без этого только что прочитанная страница выглядит
             * для вытеснения самой давней и уходит первой — кеш
             * выбрасывал бы ровно то, что читают прямо сейчас.
             */
            target.setLastModified(System.currentTimeMillis())
            send(PageLoad.Done(target))
            return@channelFlow
        }

        try {
            fetcher.download(
                url = page.uri,
                // Заголовки страницы идут в сеть как есть: Referer и
                // Origin здесь не украшение, без них хостинги отдают 403.
                headers = page.headers,
                target = target,
                /*
                 * trySend, а не send: колбэк зовётся из тела загрузки
                 * на каждый буфер, и блокировать закачку ради индикатора
                 * прогресса нельзя. Потерянный кадр прогресса безвреден,
                 * замедленная страница — нет.
                 */
                onProgress = { bytes, total -> trySend(PageLoad.Progress(bytes, total)) },
            )
            send(PageLoad.Done(target))
        } catch (e: Throwable) {
            /*
             * Ошибка уходит значением, а не исключением: контракт
             * требует терминальный Error, и читалка показывает по нему
             * экран «Повторить» вместо падения.
             */
            send(PageLoad.Error(e))
        }
    }

    override suspend fun nextChapter(chapterId: String): String? =
        ChapterPlan.neighbour(chapterIdsOf(chapterId), chapterId, +1)

    override suspend fun prevChapter(chapterId: String): String? =
        ChapterPlan.neighbour(chapterIdsOf(chapterId), chapterId, -1)

    /**
     * Имя файла — общая на весь проект формула [PageCacheKey].
     *
     * Своей копии здесь больше нет. Формулу обязаны считать одинаково
     * две стороны: источник даёт файлу имя, дисковый кеш читалки по
     * тому же имени этот файл ищет. Пока копий было две, они разошлись
     * в мелочи (склейка заголовков и пустой случай) — и кеш не находил
     * ни одного файла источника, хотя отвечал за их место.
     */
    private fun fileNameFor(page: PageRef): String = PageCacheKey.of(page)
}
