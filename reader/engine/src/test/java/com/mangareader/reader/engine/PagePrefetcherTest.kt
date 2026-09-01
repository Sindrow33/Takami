package com.mangareader.reader.engine

import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.cache.PagePrefetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Источник, считающий обращения к каждой странице. */
private class CountingSource(private val file: File) : MangaPageSource {
    val opens = ConcurrentHashMap<String, Int>()

    override suspend fun pages(chapterId: String): List<PageRef> = emptyList()

    override fun open(page: PageRef): Flow<PageLoad> = flow {
        opens.merge(page.id, 1, Int::plus)
        emit(PageLoad.Progress(1, 1))
        emit(PageLoad.Done(file))
    }

    override suspend fun nextChapter(chapterId: String): String? = null
    override suspend fun prevChapter(chapterId: String): String? = null
}

class PagePrefetcherTest {

    private fun tempFile(): File =
        Files.createTempFile("page", ".jpg").toFile().apply { writeBytes(ByteArray(8)) }

    private fun ref(id: String) = PageRef(id = id, index = 0, uri = "fake://$id")

    @Test
    fun `загруженная страница доступна вызывающему`() = runTest(UnconfinedTestDispatcher()) {
        val source = CountingSource(tempFile())
        val prefetcher = PagePrefetcher(source, this)

        prefetcher.request(ref("p1"))
        assertNotNull(prefetcher.cachedFile("p1"), "упреждающая загрузка обязана отдать файл вызывающему")
    }

    @Test
    fun `повторный запрос той же страницы не качает её дважды`() = runTest(UnconfinedTestDispatcher()) {
        val source = CountingSource(tempFile())
        val prefetcher = PagePrefetcher(source, this)

        repeat(5) { prefetcher.request(ref("p1")) }
        assertEquals(1, source.opens["p1"], "страница не должна качаться повторно — это лишний трафик")
    }

    @Test
    fun `не запрошенная страница не появляется из ниоткуда`() = runTest(UnconfinedTestDispatcher()) {
        val source = CountingSource(tempFile())
        val prefetcher = PagePrefetcher(source, this)

        prefetcher.request(ref("p1"))
        assertNull(prefetcher.cachedFile("p2"))
    }

    @Test
    fun `trimTo не выбрасывает страницы, которые попросили оставить`() =
        runTest(UnconfinedTestDispatcher()) {
            val source = CountingSource(tempFile())
            val prefetcher = PagePrefetcher(source, this)

            listOf("p1", "p2", "p3").forEach { prefetcher.request(ref(it)) }
            prefetcher.trimTo(setOf("p2", "p3"))

            // Уже загруженное остаётся доступным: trimTo отменяет
            // незавершённое, а не стирает результат.
            assertNotNull(prefetcher.cachedFile("p2"))
            assertNotNull(prefetcher.cachedFile("p3"))
        }

    @Test
    fun `ошибка источника не роняет упреждающую загрузку`() = runTest(UnconfinedTestDispatcher()) {
        val failing = object : MangaPageSource {
            override suspend fun pages(chapterId: String) = emptyList<PageRef>()
            override fun open(page: PageRef): Flow<PageLoad> = flow { throw java.io.IOException("сеть") }
            override suspend fun nextChapter(chapterId: String): String? = null
            override suspend fun prevChapter(chapterId: String): String? = null
        }
        val prefetcher = PagePrefetcher(failing, this)

        prefetcher.request(ref("p1"))
        // Фоновая загрузка молчит: ошибку покажет обычное открытие
        // страницы, когда пользователь до неё доедет.
        assertNull(prefetcher.cachedFile("p1"))
        assertTrue(true)
    }
}
