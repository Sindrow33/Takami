package com.mangareader.reader.engine.cache

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
 */
class DiskLruPageCache(
    private val directory: File,
    private var maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
) {
    companion object {
        const val DEFAULT_MAX_SIZE_BYTES = 512L * 1024 * 1024
    }

    private val mutex = Mutex()

    init {
        directory.mkdirs()
    }

    fun setMaxSizeBytes(bytes: Long) {
        maxSizeBytes = bytes
    }

    fun fileFor(uri: String): File {
        val key = sha1(uri)
        return File(directory, key)
    }

    suspend fun put(uri: String, bytes: ByteArray): File = mutex.withLock {
        val file = fileFor(uri)
        file.writeBytes(bytes)
        file.setLastModified(System.currentTimeMillis())
        evictIfNeeded()
        file
    }

    suspend fun get(uri: String): File? = mutex.withLock {
        val file = fileFor(uri)
        if (file.exists()) {
            file.setLastModified(System.currentTimeMillis())
            file
        } else null
    }

    private fun evictIfNeeded() {
        val files = directory.listFiles() ?: return
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
