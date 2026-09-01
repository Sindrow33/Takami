package dev.takami.source.web

import com.mangareader.core.model.PageRef
import core.model.ImagePage

/**
 * Чистое превращение результата автопарсера в список страниц читалки.
 *
 * Вынесено отдельной функцией над данными, без сети и без Android:
 * ровно те дефекты, которые здесь возможны — потерянный `Referer`,
 * дырявая нумерация, дубли — не видны на живом источнике до момента,
 * когда уже поздно, зато ловятся JVM-тестом на первом прогоне.
 */
object ChapterPlan {

    /**
     * @param chapterUrl URL страницы главы. Идёт в `Referer` каждой
     *   страницы: хостинги картинок массово отдают 403 запросу без него,
     *   и это не гипотетический риск, а нормальное поведение защит.
     */
    fun toPageRefs(
        chapterId: String,
        chapterUrl: String,
        images: List<ImagePage>,
    ): List<PageRef> {
        val origin = originOf(chapterUrl)

        return images
            // Порядок из парсера доверия не заслуживает: страницы могут
            // прийти из JS-массива в порядке появления в разметке.
            .sortedBy { it.index }
            // Один и тот же URL, попавший дважды, дал бы дубль страницы
            // и сбил бы нумерацию «N из M».
            .distinctBy { it.url }
            .mapIndexed { position, image ->
                PageRef(
                    // Позиция, а не index парсера: та бывает дырявой
                    // (1, 2, 5), а читалке нужен плотный 0..n-1.
                    id = "$chapterId#$position",
                    index = position,
                    uri = image.url,
                    headers = headersFor(image, chapterUrl, origin),
                    width = image.width,
                    height = image.height,
                )
            }
    }

    /**
     * Заголовки страницы. Свои значения из парсера приоритетнее: если
     * он вытащил конкретный `Referer` из разметки, он знает лучше.
     */
    private fun headersFor(
        image: ImagePage,
        chapterUrl: String,
        origin: String?,
    ): Map<String, String> = buildMap {
        put("Referer", chapterUrl)
        origin?.let { put("Origin", it) }
        putAll(image.headers)
    }

    /** `https://host/a/b?x` -> `https://host`. */
    fun originOf(url: String): String? {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd <= 0) return null
        val hostStart = schemeEnd + 3
        val hostEnd = url.indexOfFirst(hostStart) { it == '/' || it == '?' || it == '#' }
        val host = url.substring(hostStart, hostEnd)
        if (host.isEmpty()) return null
        return url.substring(0, hostStart) + host
    }

    private inline fun String.indexOfFirst(from: Int, predicate: (Char) -> Boolean): Int {
        for (i in from until length) if (predicate(this[i])) return i
        return length
    }

    /**
     * Сосед главы по списку. Возвращает null на краях — читалка по этому
     * признаку показывает «последняя глава», а не пустой экран.
     */
    fun neighbour(chapterIds: List<String>, chapterId: String, delta: Int): String? {
        val at = chapterIds.indexOf(chapterId)
        if (at < 0) return null
        return chapterIds.getOrNull(at + delta)
    }
}
