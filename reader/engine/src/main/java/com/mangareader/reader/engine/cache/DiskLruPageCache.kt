package com.mangareader.reader.engine.cache

import com.mangareader.core.model.PageRef
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

/**
 * Simple size-bounded LRU cache of downloaded page files on disk (§8:
 * "Disk LRU cache: originals of pages, inpaint patches. Configurable
 * limit, default 512MB.").
 *
 * Keyed by the page's *source* URI (this is the raw-bytes cache, keyed
 * before we know the [com.mangareader.core.model.PageKey] — that content
 * hash is only computable after at least one decode). This is distinct
 * from and much larger than the translation JSON cache, which per §8 is
 * NOT subject to this LRU eviction because it is tiny and precious.
 *
 * Ключ считается по URI **и заголовкам запроса** ([keyFor]). Один и тот
 * же URL с разным `Referer` на многих хостингах картинок отдаёт разное:
 * настоящую страницу или заглушку с 403. Ключ по одному URL склеил бы
 * их в одну запись, и в кеше навсегда осела бы заглушка.
 */
class DiskLruPageCache(
    private val directory: File,
    private var maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
) {

    /**
     * Каталоги, которые кеш не наполняет, но обязан учитывать и
     * вытеснять.
     *
     * Источник страниц сохраняет скачанные файлы сам — ему нужен путь
     * на диске, а не байты в памяти. Если оставить такой каталог без
     * присмотра, он растёт бесконечно: у источника нет ни лимита, ни
     * вытеснения, а на длинной манге это десятки гигабайт. И наоборот,
     * копировать те же байты в свой каталог значит хранить всё дважды.
     *
     * Поэтому владение местом одно: файлы остаются там, где их создал
     * источник, а лимит и LRU-вытеснение общие.
     */
    private val adoptedDirs = java.util.concurrent.CopyOnWriteArrayList<File>()

    /** Берёт чужой каталог со скачанными страницами под общий лимит. */
    fun adopt(directory: File) {
        directory.mkdirs()
        if (adoptedDirs.none { it.absolutePath == directory.absolutePath }) {
            adoptedDirs.add(directory)
        }
    }

    /** Лежит ли файл в каталоге, за который кеш уже отвечает. */
    fun isManaged(file: File): Boolean {
        val path = file.absolutePath
        if (path.startsWith(directory.absolutePath)) return true
        return adoptedDirs.any { path.startsWith(it.absolutePath) }
    }

    private fun managedFiles(): List<File> =
        (listOf(directory) + adoptedDirs).flatMap { dir ->
            dir.listFiles()?.filter { it.isFile }.orEmpty()
        }
    companion object {
        const val DEFAULT_MAX_SIZE_BYTES = 512L * 1024 * 1024

        /**
         * Заголовки, реально влияющие на тело ответа у хостингов
         * картинок. `Referer`/`Origin` — то, без чего прилетает 403;
         * `Cookie` — сессия, за которой может стоять другой контент.
         */
        private val KEYED_HEADERS = setOf("referer", "origin", "cookie")

        /** Сколько байт читаем для проверки сигнатуры при чтении. */
        private const val HEADER_PROBE_BYTES = 64
    }

    private val mutex = Mutex()

    init {
        directory.mkdirs()
    }

    fun setMaxSizeBytes(bytes: Long) {
        maxSizeBytes = bytes
    }

    /**
     * Ключ кеша: URI плюс заголовки, влияющие на ответ.
     *
     * Учитываются не все заголовки подряд — только [KEYED_HEADERS].
     * Иначе меняющийся `User-Agent` или случайный `X-Request-Id`
     * промахивались бы мимо кеша на каждом запросе, и он не работал бы
     * вовсе. Имена приводятся к нижнему регистру и сортируются, чтобы
     * порядок и регистр не порождали разные ключи для одного запроса.
     */
    fun keyFor(uri: String, headers: Map<String, String> = emptyMap()): String {
        val relevant = headers
            .filterKeys { it.lowercase() in KEYED_HEADERS }
            .map { (name, value) -> "${name.lowercase()}=$value" }
            .sorted()
        return if (relevant.isEmpty()) sha1(uri) else sha1(uri + "\n" + relevant.joinToString("\n"))
    }

    fun fileFor(uri: String, headers: Map<String, String> = emptyMap()): File =
        File(directory, keyFor(uri, headers))

    /** Файл для страницы — заголовки берутся из самой [PageRef]. */
    fun fileFor(page: PageRef): File = fileFor(page.uri, page.headers)

    /**
     * Кладёт байты страницы в кеш.
     *
     * @return файл, или null если байты не похожи на изображение.
     *
     * Проверка сигнатуры обязательна именно здесь: цена ошибки
     * несимметрична. Битую загрузку чинит повтор, а битую запись в
     * кеше — ничего, она будет отдаваться при каждом открытии главы,
     * пока пользователь не очистит кеш руками. Мусор приезжает от
     * тела, прошедшего через String, от HTML с капчей под кодом 200 и
     * от оборванной загрузки.
     */
    suspend fun put(uri: String, bytes: ByteArray, headers: Map<String, String> = emptyMap()): File? =
        mutex.withLock {
            if (!ImageBytes.looksLikeImage(bytes)) return@withLock null
            val file = fileFor(uri, headers)
            file.writeBytes(bytes)
            file.setLastModified(System.currentTimeMillis())
            evictIfNeeded()
            file
        }

    suspend fun put(page: PageRef, bytes: ByteArray): File? = put(page.uri, bytes, page.headers)

    suspend fun get(uri: String, headers: Map<String, String> = emptyMap()): File? = mutex.withLock {
        val file = fileFor(uri, headers)
        if (!file.exists() || file.length() < ImageBytes.MIN_SIZE_BYTES) return@withLock null

        // Сигнатура перечитывается и на чтении: запись могла лечь до
        // появления этой проверки или быть повреждена обрывом записи.
        // Читаем только заголовок файла, не всё тело.
        val header = ByteArray(HEADER_PROBE_BYTES)
        val read = file.inputStream().use { it.read(header) }
        if (read < ImageBytes.MIN_SIZE_BYTES || !ImageBytes.looksLikeImage(header)) {
            file.delete()
            return@withLock null
        }

        file.setLastModified(System.currentTimeMillis())
        file
    }

    suspend fun get(page: PageRef): File? = get(page.uri, page.headers)

    /** Сколько байт сейчас занято — для настроек и диагностики. */
    fun sizeBytes(): Long = managedFiles().sumOf { it.length() }

    suspend fun clear() = mutex.withLock {
        managedFiles().forEach { it.delete() }
        Unit
    }

    /**
     * Приводит общий объём под лимит. Зовётся после того, как источник
     * сам записал страницы в поднадзорный каталог: кеш о таких записях
     * не знает, и без явного вызова лимит не соблюдался бы.
     */
    suspend fun enforceLimit() = mutex.withLock { evictIfNeeded() }

    private fun evictIfNeeded() {
        val files = managedFiles()
        if (files.isEmpty()) return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= maxSizeBytes) return
        val sortedByOldest = files.sortedBy { it.lastModified() }
        for (file in sortedByOldest) {
            if (totalSize <= maxSizeBytes) break
            totalSize -= file.length()
            file.delete()
        }
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
