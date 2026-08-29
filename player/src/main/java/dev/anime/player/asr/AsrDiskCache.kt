package dev.anime.player.asr

import dev.anime.player.subtitle.SubtitleDocument
import dev.anime.player.subtitle.SubtitleFormat
import dev.anime.player.subtitle.SubtitleWriter
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Кэш готовых ASR-субтитров на диске, ключ включает движок + язык + идентичность видео,
 * чтобы результат не путался между сериями/эпизодами и переживал переустановку/обновление
 * приложения без пересчёта. Формат хранения — VTT, как в остальном плеере (см. [SubtitleWriter]).
 */
class AsrDiskCache(
    private val rootDir: File,
    private val maxEntries: Int = 100,
    private val maxBytes: Long = 16L * 1024 * 1024,
) {
    suspend fun read(key: AsrCacheKey): SubtitleDocument? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (!file.isFile) return@withContext null
        runCatching { file.setLastModified(System.currentTimeMillis()) }
        runCatching { parseVtt(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    suspend fun write(key: AsrCacheKey, document: SubtitleDocument): File = withContext(Dispatchers.IO) {
        if (!rootDir.exists()) rootDir.mkdirs()
        val file = fileFor(key)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(SubtitleWriter.write(document.copy(format = SubtitleFormat.Vtt)), Charsets.UTF_8)
        if (file.exists()) file.delete()
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
        prune()
        file
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        rootDir.deleteRecursively()
        rootDir.mkdirs()
    }

    private fun prune() {
        val files = rootDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".tmp") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        var count = 0
        var bytes = 0L
        files.forEach { file ->
            count++
            bytes += file.length()
            if (count > maxEntries || bytes > maxBytes) runCatching { file.delete() }
        }
    }

    private fun fileFor(key: AsrCacheKey): File {
        if (!rootDir.exists()) rootDir.mkdirs()
        return File(rootDir, "${key.hash()}.vtt")
    }

    /** Минимальный VTT-парсер: обратное преобразование к [SubtitleWriter.write], не общего назначения. */
    private fun parseVtt(raw: String): SubtitleDocument {
        val lines = raw.lineSequence().toList()
        val cues = mutableListOf<dev.anime.player.subtitle.SubtitleCue>()
        var i = 0
        var index = 1
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val (startStr, endStr) = line.split("-->").map { it.trim() }
                val start = parseVttTime(startStr)
                val end = parseVttTime(endStr)
                val text = StringBuilder()
                i++
                while (i < lines.size && lines[i].isNotBlank()) {
                    if (text.isNotEmpty()) text.append('\n')
                    text.append(lines[i])
                    i++
                }
                cues += dev.anime.player.subtitle.SubtitleCue(index++, start, end, text.toString())
            }
            i++
        }
        return SubtitleDocument(cues, SubtitleFormat.Vtt)
    }

    private fun parseVttTime(raw: String): Long {
        val parts = raw.split(":")
        val (h, m, secMs) = when (parts.size) {
            3 -> Triple(parts[0].toLong(), parts[1].toLong(), parts[2])
            else -> Triple(0L, parts[0].toLong(), parts[1])
        }
        val secParts = secMs.split(".")
        val s = secParts[0].toLong()
        val ms = secParts.getOrElse(1) { "0" }.padEnd(3, '0').take(3).toLong()
        return ((h * 3600 + m * 60 + s) * 1000) + ms
    }
}

data class AsrCacheKey(
    val videoIdentity: String,
    val engineFingerprint: String,
    val language: String,
) {
    fun hash(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        listOf(videoIdentity, engineFingerprint, language.lowercase()).forEach {
            digest.update(it.toByteArray(Charsets.UTF_8))
            digest.update(0)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
