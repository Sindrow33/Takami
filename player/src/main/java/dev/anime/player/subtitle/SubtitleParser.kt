package dev.anime.player.subtitle

/**
 * Разбор SRT и VTT в общую модель.
 *
 * Нужен озвучке: текст реплик для TTS берётся из субтитров, а рядом со
 * скачанной серией почти всегда лежит `.srt` или `.ass`. Ждать ASR ради
 * файла, у которого субтитры уже есть, — это заставлять пользователя
 * ждать распознавание того, что уже написано.
 *
 * Чистые функции над строками, поэтому проверяемо тестом.
 */
object SubtitleParser {

    /** Расширения, которые умеем читать. */
    val SUPPORTED_EXTENSIONS = setOf("srt", "vtt")

    fun isSupported(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS

    fun parse(raw: String): SubtitleDocument {
        val cues = mutableListOf<SubtitleCue>()
        var index = 1
        val lines = raw.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!line.contains("-->")) {
                i++
                continue
            }
            val parts = line.split("-->")
            val start = parseTime(parts[0])
            // Отсекаем настройки позиционирования VTT после времени конца:
            // "00:00:02.000 align:start line:90%" — иначе время не разберётся.
            val end = parseTime(parts.getOrElse(1) { "" }.trim().substringBefore(' '))
            if (start == null || end == null) {
                i++
                continue
            }
            val text = StringBuilder()
            i++
            while (i < lines.size && lines[i].isNotBlank()) {
                if (text.isNotEmpty()) text.append(' ')
                text.append(stripTags(lines[i].trim()))
                i++
            }
            val body = text.toString().trim()
            if (body.isNotEmpty() && end > start) {
                cues.add(SubtitleCue(index++, start, end, body))
            }
        }
        return SubtitleDocument(cues.sortedBy { it.startMs }, SubtitleFormat.Vtt)
    }

    /**
     * `00:01:02,500` (SRT) и `00:01:02.500` (VTT), а также `01:02.500`
     * без часов — VTT это допускает, и такие файлы встречаются.
     */
    fun parseTime(raw: String): Long? {
        val text = raw.trim().replace(',', '.')
        if (text.isEmpty()) return null
        val segments = text.split(':')
        if (segments.size !in 2..3) return null
        return runCatching {
            val seconds = segments.last().toDouble()
            val minutes = segments[segments.size - 2].toLong()
            val hours = if (segments.size == 3) segments[0].toLong() else 0L
            ((hours * 3600 + minutes * 60) * 1000L) + (seconds * 1000).toLong()
        }.getOrNull()
    }

    /**
     * Убирает разметку: HTML-теги VTT (`<i>`) и ASS-фигурные блоки (`{\pos}`).
     * Для озвучки это критично — TTS иначе честно произносит теги вслух.
     */
    fun stripTags(text: String): String = text
        .replace(Regex("<[^>]*>"), "")
        .replace(Regex("\\{[^}]*\\}"), "")
        .trim()

    /**
     * Имя файла субтитров рядом с видео: то же имя, другое расширение.
     * `01 - Начало.mkv` → `01 - Начало.srt`.
     */
    fun sidecarNames(videoFileName: String): List<String> {
        val base = videoFileName.substringBeforeLast('.', videoFileName)
        return SUPPORTED_EXTENSIONS.map { base + "." + it }
    }
}
