package com.mangareader.reader.engine.decode

import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Tiled access to a single very-tall webtoon page image (§6).
 *
 * Rather than decoding the whole strip (which for a 15000px webtoon page
 * at full width can be hundreds of MB as ARGB_8888), this class slices the
 * page into fixed-height tiles ([tileHeightPx], default ~2000px) decoded
 * on demand via [BitmapRegionDecoder], and keeps only a small sliding
 * window of tiles ([maxCachedTiles], default 4) in memory — evicting the
 * ones farthest from the currently visible tile index.
 *
 * This is intentionally decode-cache only; it holds no knowledge of scroll
 * position beyond what [ensureWindow] is told to prepare.
 */
class TiledPage(
    private val file: File,
    val pageWidth: Int,
    val pageHeight: Int,
    private val tileHeightPx: Int = 2000,
    private val maxCachedTiles: Int = 4,
) {
    val tileCount: Int = ((pageHeight + tileHeightPx - 1) / tileHeightPx).coerceAtLeast(1)

    private val mutex = Mutex()
    private var regionDecoder: BitmapRegionDecoder? = null
    private val tiles = ConcurrentHashMap<Int, Bitmap>()
    private val lru = ArrayDeque<Int>() // most-recently-used at the end

    fun tileRect(tileIndex: Int): Rect {
        val top = tileIndex * tileHeightPx
        val bottom = minOf(pageHeight, top + tileHeightPx)
        return Rect(0, top, pageWidth, bottom)
    }

    fun tileIndexForY(y: Int): Int = (y / tileHeightPx).coerceIn(0, tileCount - 1)

    /** Returns the cached tile bitmap if present, without triggering a decode. */
    fun peekTile(tileIndex: Int): Bitmap? = tiles[tileIndex]

    /**
     * Ensures tiles around [centerTileIndex] (± [radius], default 1 => up
     * to 3 tiles: prev/current/next, matching the "3-4 tile window" spec)
     * are decoded and cached, evicting anything outside that window.
     */
    suspend fun ensureWindow(centerTileIndex: Int, radius: Int = 1) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val wanted = (centerTileIndex - radius..centerTileIndex + radius)
                .filter { it in 0 until tileCount }
                .toSet()

            // Evict anything not wanted and beyond capacity policy.
            val toEvict = tiles.keys.filter { it !in wanted }
            for (idx in toEvict) {
                tiles.remove(idx)?.let { if (!it.isRecycled) it.recycle() }
                lru.remove(idx)
            }

            for (idx in wanted) {
                if (!tiles.containsKey(idx)) {
                    decodeTile(idx)?.let { bmp ->
                        tiles[idx] = bmp
                        lru.remove(idx)
                        lru.addLast(idx)
                    }
                } else {
                    lru.remove(idx)
                    lru.addLast(idx)
                }
            }

            // Hard cap safety net even if radius grows in a future caller.
            while (tiles.size > maxCachedTiles && lru.isNotEmpty()) {
                val evictIdx = lru.removeFirst()
                tiles.remove(evictIdx)?.let { if (!it.isRecycled) it.recycle() }
            }
        }
    }

    private fun decodeTile(tileIndex: Int): Bitmap? {
        val decoder = regionDecoder ?: PageDecoder.openRegionDecoder(file).also { regionDecoder = it }
        val rect = tileRect(tileIndex)
        return decoder.decodeRegion(rect, null)
    }

    fun close() {
        tiles.values.forEach { if (!it.isRecycled) it.recycle() }
        tiles.clear()
        lru.clear()
        regionDecoder?.recycle()
        regionDecoder = null
    }
}
