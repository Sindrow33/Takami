package dev.anime.player.dub

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Кэш синтезированных аудио-клипов на диске: TTS — самая дорогая (время+деньги на облачные
 * голоса) операция в пайплайне озвучки, поэтому кэшируем по хэшу (текст+голос+движок),
 * а не по номеру реплики — одинаковые фразы в разных сериях не пересинтезируются.
 */
class DubDiskCache(
    private val rootDir: File,
    private val maxEntries: Int = 500,
    private val maxBytes: Long = 64L * 1024 * 1024,
) {
    suspend fun read(key: DubCacheKey): CachedClip? = withContext(Dispatchers.IO) {
        val audioFile = fileFor(key, "audio")
        val metaFile = fileFor(key, "meta")
        if (!audioFile.isFile || !metaFile.isFile) return@withContext null
        runCatching { audioFile.setLastModified(System.currentTimeMillis()) }
        val meta = metaFile.readText(Charsets.UTF_8).split('\n')
        val mimeType = meta.getOrNull(0) ?: return@withContext null
        val durationMs = meta.getOrNull(1)?.toLongOrNull() ?: return@withContext null
        CachedClip(audioFile.readBytes(), mimeType, durationMs)
    }

    suspend fun write(key: DubCacheKey, output: TtsProvider.SynthesisOutput): Unit = withContext(Dispatchers.IO) {
        if (!rootDir.exists()) rootDir.mkdirs()
        val audioFile = fileFor(key, "audio")
        val metaFile = fileFor(key, "meta")
        writeAtomically(audioFile, output.audio)
        writeAtomically(metaFile, "${output.mimeType}\n${output.durationMs}".toByteArray(Charsets.UTF_8))
        prune()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        rootDir.deleteRecursively()
        rootDir.mkdirs()
    }

    private fun writeAtomically(file: File, bytes: ByteArray) {
        val tmp = File(file.parentFile ?: rootDir, file.name + ".tmp")
        tmp.writeBytes(bytes)
        if (file.exists()) file.delete()
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun prune() {
        val files = rootDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".tmp") && it.name.endsWith(".audio") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        var count = 0
        var bytes = 0L
        files.forEach { audio ->
            count++
            bytes += audio.length()
            if (count > maxEntries || bytes > maxBytes) {
                runCatching { audio.delete() }
                runCatching { File(audio.parentFile, audio.nameWithoutExtension + ".meta").delete() }
            }
        }
    }

    private fun fileFor(key: DubCacheKey, suffix: String): File {
        if (!rootDir.exists()) rootDir.mkdirs()
        return File(rootDir, "${key.hash()}.$suffix")
    }
}

data class CachedClip(val audio: ByteArray, val mimeType: String, val durationMs: Long)

data class DubCacheKey(
    val text: String,
    val voiceId: String,
    val providerFingerprint: String,
) {
    fun hash(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        listOf(text, voiceId, providerFingerprint).forEach {
            digest.update(it.toByteArray(Charsets.UTF_8))
            digest.update(0)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
