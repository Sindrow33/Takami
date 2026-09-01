package dev.takami.app.parser

import android.content.Context
import core.engine.ParseEngine
import core.extract.RequestKind
import core.model.ContentProfile
import core.model.MediaKind
import core.model.ParsedPayload
import core.model.SourceConfig
import core.model.TerminalContent
import core.net.OkHttpClientAdapter
import core.store.SourceRegistry
import core.validate.Verdict
import dev.takami.app.swipes.DiscoveredTitles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Разбор одной страницы автопарсером — то, что стоит за кнопкой
 * «Разобрать».
 *
 * Держит результат в человекочитаемом виде: экрану не нужны ни
 * `Verdict`, ни `ParsedPayload`, ему нужно несколько строк, которые
 * можно показать и переслать нам при разборе проблемы.
 */
class SourceProber(private val context: Context) {

    data class Report(val ok: Boolean, val lines: List<String>)

    suspend fun probe(url: String): Report = withContext(Dispatchers.IO) {
        val host = hostOf(url) ?: return@withContext Report(false, listOf("Не разобрать адрес: $url"))

        runCatching {
            val client = OkHttpClientAdapter()
            val response = client.get(url, client.browserHeaders(referer = url))
            if (!response.isSuccess) {
                return@withContext Report(
                    false,
                    listOf(
                        "Сайт ответил HTTP ${response.code}.",
                        if (response.code == 403) {
                            "403 обычно значит защиту от ботов: понадобится прокси или проверка браузера."
                        } else {
                            "Проверьте ссылку и доступность сайта."
                        },
                    ),
                )
            }

            val registry = SourceRegistry(File(context.filesDir, "sources"))
            val engine = ParseEngine(registry)

            /*
             * Пробуем как страницу главы, и если содержимого нет — как
             * список глав. Порядок такой, потому что пользователь чаще
             * вставляет ссылку на то, что открыл и хочет читать.
             */
            val bundled = SourceConfig(host = host, profile = ContentProfile.of(MediaKind.MANGA))
            val lines = mutableListOf<String>()
            var ok = false

            for (kind in listOf(RequestKind.CONTENT, RequestKind.UNITS, RequestKind.LISTING)) {
                val result = engine.parse(
                    host = host,
                    html = response.body,
                    url = url,
                    bundled = bundled,
                    kind = kind,
                    truncated = response.truncated,
                    browserHeaders = true,
                )
                result.configToPersist?.let(registry::persist)

                val found = describe(result.payload)
                if (result.isUsable && found != null) {
                    ok = true
                    lines += found
                    /*
                     * Разобранный каталог сохраняем для подбора свайпами.
                     * Без этого разбор сайта оставался диагностикой: он
                     * печатал «каталог: 40 тайтлов» и выбрасывал их, а
                     * экран свайпов оставался пустым навсегда.
                     */
                    val listing = result.payload as? ParsedPayload.Listing
                    if (listing != null && listing.items.isNotEmpty()) {
                        DiscoveredTitles(context).add(listing.items, host)
                        lines += "Тайтлы добавлены в подбор свайпами."
                    }
                    lines += "Уверенность: ${percent(result.report.verdict.confidenceOrZero)}."
                    result.heal?.takeIf { !it.isEmpty }?.let {
                        lines += "Селекторы были сломаны — парсер предложил замену."
                    }
                    break
                }
            }

            if (!ok) {
                /*
                 * Про скрипты говорим только при доказательствах.
                 *
                 * Раньше эта строка печаталась при любой неудаче, и
                 * из-за неё живой сайт со статическим каталогом
                 * выглядел как SPA: разбор падал по своей причине, а
                 * виноватым назначался сайт. Признак «страницу собирают
                 * скрипты» — это мало разметки при том, что теги
                 * скриптов есть; всё остальное честнее назвать
                 * непонятым.
                 */
                lines += "Разметку сняли, но ни глав, ни списка тайтлов не нашли."
                val looksDynamic = response.body.length < DYNAMIC_HTML_LIMIT &&
                    response.body.contains("<script", ignoreCase = true)
                lines += if (looksDynamic) {
                    "Похоже, страница собирается скриптами: разметки мало, а скрипты есть."
                } else {
                    "Разметка на месте — её не удалось разобрать. Пришлите ссылку разработчику."
                }
                lines += "Размер ответа: ${response.bodySize} символов" +
                    if (response.truncated) " (обрезан по лимиту)." else "."
            }

            Report(ok, lines)
        }.getOrElse { t ->
            Report(false, listOf("Не удалось: ${t.message ?: t::class.simpleName}"))
        }
    }

    /** Что именно нашли — в одну строку, без внутренних типов. */
    private fun describe(payload: ParsedPayload): String? = when (payload) {
        is ParsedPayload.Content -> when (val content = payload.content) {
            is TerminalContent.Images ->
                "Глава: ${content.pages.size} страниц.".takeIf { content.pages.isNotEmpty() }
            is TerminalContent.Text ->
                "Текст: ${content.chapter.paragraphs.size} абзацев."
                    .takeIf { content.chapter.paragraphs.isNotEmpty() }
            else -> null
        }
        is ParsedPayload.Units ->
            "Список глав: ${payload.units.size}.".takeIf { payload.units.isNotEmpty() }
        is ParsedPayload.Listing ->
            "Каталог: ${payload.items.size} тайтлов.".takeIf { payload.items.isNotEmpty() }
        is ParsedPayload.Entry -> "Страница тайтла разобрана."
    }

    /** Ниже этого объёма страница без содержимого выглядит как оболочка SPA. */
    private val DYNAMIC_HTML_LIMIT = 15_000

    private fun percent(value: Double): String = "${(value * 100).toInt()}%"

    private fun hostOf(url: String): String? {
        val afterScheme = url.substringAfter("://", "")
        if (afterScheme.isEmpty()) return null
        return afterScheme.substringBefore('/').substringBefore('?').ifEmpty { null }
    }
}
