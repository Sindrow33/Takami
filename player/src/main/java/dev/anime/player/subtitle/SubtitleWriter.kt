package dev.anime.player.subtitle

/** Сериализация [SubtitleDocument] в SRT/VTT — то, что реально можно отдать в [dev.anime.player.core.PlayerEngine.addSubtitleTrack]. */
object SubtitleWriter {

    fun write(document: SubtitleDocument): String = when (document.format) {
        SubtitleFormat.Srt -> writeSrt(document)
        SubtitleFormat.Vtt -> writeVtt(document)
    }

    private fun writeVtt(document: SubtitleDocument): String = buildString {
        append("WEBVTT\n\n")
        document.cues.forEach { cue ->
            append(cue.index).append('\n')
            append(formatVttTime(cue.startMs)).append(" --> ").append(formatVttTime(cue.endMs)).append('\n')
            append(cue.text).append("\n\n")
        }
    }

    private fun writeSrt(document: SubtitleDocument): String = buildString {
        document.cues.forEach { cue ->
            append(cue.index).append('\n')
            append(formatSrtTime(cue.startMs)).append(" --> ").append(formatSrtTime(cue.endMs)).append('\n')
            append(cue.text).append("\n\n")
        }
    }

    private fun formatVttTime(ms: Long): String {
        val parts = split(ms)
        return "%02d:%02d:%02d.%03d".format(parts.hours, parts.minutes, parts.seconds, parts.millis)
    }

    private fun formatSrtTime(ms: Long): String {
        val parts = split(ms)
        return "%02d:%02d:%02d,%03d".format(parts.hours, parts.minutes, parts.seconds, parts.millis)
    }

    private data class TimeParts(val hours: Int, val minutes: Int, val seconds: Int, val millis: Int)

    private fun split(ms: Long): TimeParts {
        val clamped = ms.coerceAtLeast(0L)
        val totalSeconds = clamped / 1000
        val millis = (clamped % 1000).toInt()
        val h = (totalSeconds / 3600).toInt()
        val m = ((totalSeconds % 3600) / 60).toInt()
        val s = (totalSeconds % 60).toInt()
        return TimeParts(h, m, s, millis)
    }
}
