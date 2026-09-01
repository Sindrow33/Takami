package dev.takami.swipes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Загрузка обложек для карточек.
 *
 * Своя, а не Coil/Glide: библиотека изображений — это ещё несколько мегабайт
 * в APK, а вес сборки в проекте уже был блокером релиза. Нужно ровно одно —
 * показать обложку в карточке, и это сотня строк.
 *
 * Двухуровневый кеш: в памяти на текущую колоду и на диске между запусками.
 * Без диска колода перекачивала бы обложки при каждом открытии вкладки.
 */
class CoverLoader(
    private val cacheDir: File,
    private val maxMemoryEntries: Int = 12,
    private val maxDiskBytes: Long = 8L * 1024 * 1024,
) {
    private val memory = LinkedHashMap<String, Bitmap>()
    private val mutex = Mutex()

    suspend fun load(url: String, maxWidthPx: Int): Bitmap? {
        if (url.isBlank()) return null
        val key = keyOf(url)

        mutex.withLock { memory[key] }?.let { return it }

        return withContext(Dispatchers.IO) {
            val file = File(cacheDir.apply { mkdirs() }, key)
            val bytes = if (file.isFile && file.length() > 0) {
                // Отметка времени обязательна: вытеснение идёт по LRU от
                // lastModified, а чтение файла его не меняет — без этого
                // только что показанная обложка выглядит самой давней.
                runCatching { file.setLastModified(System.currentTimeMillis()) }
                runCatching { file.readBytes() }.getOrNull()
            } else {
                download(url)?.also { data ->
                    runCatching {
                        val part = File(file.parentFile, file.name + ".part")
                        part.writeBytes(data)
                        if (!part.renameTo(file)) {
                            part.copyTo(file, overwrite = true)
                            part.delete()
                        }
                        prune()
                    }
                }
            } ?: return@withContext null

            val bitmap = decodeScaled(bytes, maxWidthPx) ?: return@withContext null
            mutex.withLock {
                memory[key] = bitmap
                while (memory.size > maxMemoryEntries) {
                    val oldest = memory.keys.firstOrNull() ?: break
                    memory.remove(oldest)
                }
            }
            bitmap
        }
    }

    private fun download(url: String): ByteArray? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            // Часть сайтов отдаёт обложки только с браузерным User-Agent.
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) Takami/1.0",
            )
            if (connection.responseCode !in 200..299) return@runCatching null
            val length = connection.contentLength
            if (length > MAX_COVER_BYTES) return@runCatching null
            connection.inputStream.use { it.readBytes().takeIf { b -> b.size <= MAX_COVER_BYTES } }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    /**
     * Декодирует с прореживанием: обложка в оригинале бывает 1200px, а в
     * карточку идёт ~500. Полный битмап на каждую карточку — это десятки
     * мегабайт в памяти и заметный рывок анимации на слабом телефоне.
     */
    private fun decodeScaled(bytes: ByteArray, maxWidthPx: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, maxWidthPx)
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }.getOrNull()

    private fun prune() {
        val files = cacheDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".part") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        var bytes = 0L
        files.forEach { file ->
            bytes += file.length()
            if (bytes > maxDiskBytes) runCatching { file.delete() }
        }
    }

    private fun keyOf(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    companion object {
        /** Больше этого обложка не бывает; защита от подстановки гигантского файла. */
        const val MAX_COVER_BYTES = 4 * 1024 * 1024

        /**
         * Степень двойки, при которой ширина укладывается в целевую.
         * Вынесено отдельно и проверено тестом: ошибка здесь либо съедает
         * память, либо превращает обложку в кашу.
         */
        fun sampleSizeFor(sourceWidth: Int, targetWidth: Int): Int {
            if (sourceWidth <= 0 || targetWidth <= 0) return 1
            var sample = 1
            while (sourceWidth / (sample * 2) >= targetWidth) sample *= 2
            return sample
        }
    }
}
