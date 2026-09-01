package core.terminal

import core.model.AudioTrack
import core.model.BlockReason
import core.model.StreamKind
import core.model.Subtitle
import core.model.TerminalContent
import core.model.TerminalSpec
import core.model.VideoStream
import core.parse.Dom
import core.parse.UrlTools

/* ═══════════════════════════════════════════════════════════════════
   ВИДЕОПОТОКИ
   Ссылку на поток почти никогда не встретить в разметке: она либо в
   inline-скрипте инициализации плеера, либо приходит отдельным JSON.
   Здесь разбирается первый случай — того, что видно в HTML страницы.
   Второй случай (плеер во фрейме, запрос за манифестом) требует сети
   и остаётся за вызывающим: мы честно возвращаем адрес фрейма.
   ═══════════════════════════════════════════════════════════════════ */

class StreamResolver {

    fun resolve(dom: Dom, spec: TerminalSpec?, baseUrl: String): TerminalContent {
        val headers = buildHeaders(spec, baseUrl)

        // 1. Прямой <video><source> — редко, но бывает и стоит дёшево.
        val direct = dom.select("video source, video[src]").mapNotNull { n ->
            val src = n.attr("src").ifBlank { n.absUrl("src") }
            if (src.isBlank()) null
            else VideoStream(
                url = UrlTools.absolutize(src, baseUrl),
                kind = kindOf(src),
                quality = n.attr("label").takeIf { it.isNotBlank() },
                headers = headers,
            )
        }
        if (direct.isNotEmpty()) return TerminalContent.Streams(dedupe(direct))

        // 2. Манифесты и файлы в inline-скриптах.
        val fromScripts = scanScripts(dom, baseUrl, headers)
        if (fromScripts.isNotEmpty()) return TerminalContent.Streams(dedupe(fromScripts))

        // 3. Плеер во фрейме: сами достать поток не можем, но обязаны
        //    сказать, куда идти дальше, а не молча вернуть пустоту.
        frameUrl(dom, spec, baseUrl)?.let { frame ->
            return TerminalContent.Unavailable(
                BlockReason.UNKNOWN,
                "плеер во внешнем фрейме, нужен отдельный запрос: $frame",
            )
        }

        return TerminalContent.Unavailable(BlockReason.UNKNOWN, "потоки не найдены в разметке страницы")
    }

    /**
     * Ищем в скриптах строки, похожие на манифест или медиафайл.
     * Ключевой фильтр — расширение и характерные слова: без него
     * в выдачу лезут пути к картинкам и статике.
     */
    internal fun scanScripts(dom: Dom, baseUrl: String, headers: Map<String, String>): List<VideoStream> {
        val out = ArrayList<VideoStream>()
        for (script in dom.inlineScripts()) {
            if (script.length > MAX_SCRIPT) continue
            if (HINTS.none { script.contains(it, ignoreCase = true) }) continue

            for (m in URL_LITERAL.findAll(script)) {
                val raw = m.groupValues[1].replace("\\/", "/")
                if (!MEDIA_EXT.containsMatchIn(raw)) continue
                val abs = UrlTools.absolutize(raw, baseUrl)
                out += VideoStream(
                    url = abs,
                    kind = kindOf(abs),
                    quality = qualityNear(script, m.range.first),
                    height = heightOf(abs),
                    headers = headers,
                    isMaster = abs.contains("master", ignoreCase = true) ||
                        abs.endsWith(".m3u8", ignoreCase = true),
                )
            }
        }
        return out
    }

    /** Метка качества обычно стоит рядом со ссылкой: {"720p": "...m3u8"}. */
    private fun qualityNear(script: String, at: Int): String? {
        val from = (at - 60).coerceAtLeast(0)
        val window = script.substring(from, at)
        return QUALITY.findAll(window).lastOrNull()?.value
    }

    private fun heightOf(url: String): Int? =
        QUALITY.find(url)?.value?.removeSuffix("p")?.toIntOrNull()

    private fun kindOf(url: String): StreamKind {
        val low = url.substringBefore('?').lowercase()
        return when {
            low.endsWith(".m3u8") -> StreamKind.HLS
            low.endsWith(".mpd") -> StreamKind.DASH
            else -> StreamKind.PROGRESSIVE
        }
    }

    private fun frameUrl(dom: Dom, spec: TerminalSpec?, baseUrl: String): String? {
        spec?.playerSelectors?.forEach { sel ->
            dom.selectFirst(sel)?.let { n ->
                val src = n.attr("src").ifBlank { n.attr("data-src") }
                if (src.isNotBlank()) return UrlTools.absolutize(src, baseUrl)
            }
        }
        return dom.select("iframe[src]")
            .map { it.attr("src") }
            .firstOrNull { src -> PLAYER_HINTS.any { src.contains(it, ignoreCase = true) } }
            ?.let { UrlTools.absolutize(it, baseUrl) }
    }

    /**
     * Referer и Origin: без них большинство CDN отдают 403.
     * Ставим их всегда, даже если конфиг молчит — вреда нет,
     * а забытый Referer выглядит как «поток не работает».
     */
    private fun buildHeaders(spec: TerminalSpec?, baseUrl: String): Map<String, String> = buildMap {
        putAll(spec?.requiredHeaders.orEmpty())
        putIfAbsent("Referer", baseUrl)
        core.model.originOf(baseUrl)?.let { putIfAbsent("Origin", it) }
    }

    /** Один и тот же URL в нескольких качествах — оставляем лучший. */
    private fun dedupe(streams: List<VideoStream>): List<VideoStream> {
        val byUrl = LinkedHashMap<String, VideoStream>()
        for (s in streams) {
            val prev = byUrl[s.url]
            if (prev == null || (s.height ?: 0) > (prev.height ?: 0)) byUrl[s.url] = s
        }
        return byUrl.values.sortedByDescending { it.height ?: 0 }
    }

    private companion object {
        const val MAX_SCRIPT = 400_000
        val URL_LITERAL = Regex("""["'](https?:[^"']{8,600}|/[^"']{8,600})["']""")
        val MEDIA_EXT = Regex("""\.(m3u8|mpd|mp4|webm)(\?|$|["'\\])""", RegexOption.IGNORE_CASE)
        val QUALITY = Regex("""\b(2160|1440|1080|720|480|360|240)p?\b""")
        val HINTS = listOf("m3u8", "mpd", "playlist", "source", "file:", "hls", "video")
        val PLAYER_HINTS = listOf("player", "embed", "kodik", "video", "iframe.")
    }
}
