package dev.takami.app.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageCacheKey
import com.mangareader.core.model.PageLoad
import com.mangareader.core.model.PageRef
import com.mangareader.feature.reader.ReaderSourceRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Библиотека в папке, выбранной пользователем.
 *
 * Раскладка та же, что у внутреннего каталога:
 * ```
 * <папка>/<Название>/<Глава>/0001.jpg
 * <папка>/<Название>/<Глава>.cbz
 * ```
 * Отличие одно, но принципиальное: доступ идёт через `content://`, а не
 * по пути на диске — у выбранной через системный диалог папки пути,
 * пригодного для `File`, может не быть вовсе (SD-карта, облачный
 * провайдер, USB-накопитель).
 */
object TreeLibrary {

    fun titles(context: Context, tree: Uri): List<LocalLibrary.Title> {
        val root = DocumentFile.fromTreeUri(context, tree) ?: return emptyList()
        return root.listFiles()
            .filter { it.isDirectory }
            .sortedBy { it.name.orEmpty() }
            .map { dir ->
                val name = dir.name.orEmpty()
                LocalLibrary.Title(
                    id = name,
                    name = name,
                    // File здесь неприменим: у content-документа пути нет.
                    // Поле остаётся ради общей модели с локальной
                    // библиотекой, экраны его для этих тайтлов не читают.
                    dir = File(name),
                    chapters = chapters(dir),
                    treeUri = dir.uri,
                )
            }
            .filter { it.chapters.isNotEmpty() }
    }

    private fun chapters(titleDir: DocumentFile): List<LocalLibrary.Chapter> {
        val entries = titleDir.listFiles()
        val nested = entries
            .filter { it.isDirectory || LibraryNames.isArchive(it.name.orEmpty()) }
            .sortedBy { it.name.orEmpty() }
            .mapIndexed { index, entry ->
                val raw = entry.name.orEmpty()
                val name = if (entry.isDirectory) raw else raw.substringBeforeLast('.')
                LocalLibrary.Chapter(
                    id = raw,
                    name = name,
                    number = LibraryNames.numberOf(name) ?: (index + 1).toFloat(),
                    isArchive = !entry.isDirectory,
                )
            }
        if (nested.isNotEmpty()) return nested

        /*
         * Плоская раскладка: страницы лежат прямо в папке тайтла, без
         * подпапки главы. Так выглядит почти всё, что скачано одним
         * архивом и распаковано на месте, и так же — папка, которую
         * пользователь просто перетащил с компьютера. Без этой ветки
         * тайтл виден, а глав у него ноль, и читалку открыть не из
         * чего.
         */
        val hasPages = entries.any { it.isFile && LibraryNames.isImage(it.name.orEmpty()) }
        return if (hasPages) {
            listOf(
                LocalLibrary.Chapter(
                    id = FLAT_CHAPTER_ID,
                    name = titleDir.name.orEmpty().ifEmpty { "Глава" },
                    number = 1f,
                    isArchive = false,
                ),
            )
        } else {
            emptyList()
        }
    }

    /** Идентификатор главы, страницы которой лежат в самой папке тайтла. */
    const val FLAT_CHAPTER_ID = "."

    /**
     * Источник страниц поверх выбранной папки.
     *
     * Читалка требует от `open()` готовый файл на диске, поэтому
     * страница один раз копируется в кеш приложения. Каталог копий
     * отдаётся дисковому кешу через `adopt`: иначе он растёт без
     * предела — у самого источника ни лимита, ни вытеснения нет, и на
     * длинной манге это гигабайты, которые никто не убирает.
     */
    fun sourceFor(context: Context, title: LocalLibrary.Title): MangaPageSource =
        TreePageSource(context, requireNotNull(title.treeUri), cacheDir(context))

    private fun cacheDir(context: Context): File =
        File(context.cacheDir, "tree-pages").apply {
            mkdirs()
            ReaderSourceRegistry.diskCache?.adopt(this)
        }
}

/** Разбор имён — чистые функции над строками, проверяемые в JVM. */
object LibraryNames {

    private val ARCHIVE_EXTENSIONS = setOf("cbz", "zip")
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")

    fun isArchive(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in ARCHIVE_EXTENSIONS

    fun isImage(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

    fun numberOf(name: String): Float? =
        Regex("""\d+(?:[.,]\d+)?""").find(name)?.value?.replace(',', '.')?.toFloatOrNull()

    /**
     * Порядок страниц внутри главы.
     *
     * По имени, но с числом как числом: при обычной сортировке строк
     * `10.jpg` встаёт перед `2.jpg`, и глава читается в перепутанном
     * порядке. Сканы сплошь и рядом лежат без ведущих нулей, так что
     * это не редкий случай, а обычный.
     */
    fun pageOrder(names: List<String>): List<String> =
        names.sortedWith(compareBy({ numberOf(it) ?: Float.MAX_VALUE }, { it.lowercase() }))
}

private class TreePageSource(
    private val context: Context,
    private val titleTree: Uri,
    private val cacheDir: File,
) : MangaPageSource {

    private val chapterOrder = mutableListOf<String>()

    override suspend fun pages(chapterId: String): List<PageRef> {
        val title = DocumentFile.fromTreeUri(context, titleTree) ?: return emptyList()
        if (chapterOrder.isEmpty()) {
            chapterOrder += title.listFiles().mapNotNull { it.name }.sorted()
        }

        // Плоская раскладка: страницы лежат в самой папке тайтла.
        val chapter = if (chapterId == TreeLibrary.FLAT_CHAPTER_ID) {
            title
        } else {
            title.listFiles().firstOrNull { it.name == chapterId } ?: return emptyList()
        }

        /*
         * Глава-архив. Раньше здесь стоял выход по пустому списку для
         * всего, что не папка, — то есть CBZ-глава давала пустую
         * читалку: тайтл в списке есть, глава есть, а страниц ноль.
         * Локальная библиотека архивы читала всегда, и потерялись они
         * ровно на переходе к выбранной папке.
         */
        if (!chapter.isDirectory) {
            return archivePages(chapterId, chapter)
        }

        val files = chapter.listFiles().filter { it.isFile && LibraryNames.isImage(it.name.orEmpty()) }
        val byName = files.associateBy { it.name.orEmpty() }
        return LibraryNames.pageOrder(byName.keys.toList()).mapIndexedNotNull { index, name ->
            val doc = byName[name] ?: return@mapIndexedNotNull null
            PageRef(id = "$chapterId/$name", index = index, uri = doc.uri.toString())
        }
    }

    /**
     * Страницы CBZ/ZIP, лежащего в выбранной папке.
     *
     * Архив распаковывается один раз целиком, а не по странице: у
     * content-документа нет пути на диске, и открыть его как zip с
     * произвольным доступом нельзя — только читать потоком с начала.
     * Поэтому распаковка по одной странице означала бы перечитывание
     * всего архива на каждую страницу.
     */
    private fun archivePages(chapterId: String, archive: DocumentFile): List<PageRef> {
        val dir = File(cacheDir, "cbz/" + PageCacheKey.of(archive.uri.toString()))
        val marker = File(dir, ".done")
        if (!marker.isFile) {
            dir.mkdirs()
            runCatching {
                context.contentResolver.openInputStream(archive.uri).use { input ->
                    requireNotNull(input) { "архив недоступен: ${archive.uri}" }
                    java.util.zip.ZipInputStream(input.buffered()).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            val name = entry.name
                            if (entry.isDirectory || !LibraryNames.isImage(name)) continue
                            // Имя записи может содержать путь и «..» —
                            // распаковка по нему как есть писала бы за
                            // пределы каталога.
                            val out = File(dir, name.replace('/', '_').replace("..", "_"))
                            out.outputStream().buffered().use { zip.copyTo(it) }
                        }
                    }
                }
            }.onFailure { return emptyList() }
            marker.writeBytes(ByteArray(0))
        }

        val names = dir.listFiles()?.filter { it.isFile && LibraryNames.isImage(it.name) }.orEmpty()
            .associateBy { it.name }
        return LibraryNames.pageOrder(names.keys.toList()).mapIndexedNotNull { index, name ->
            val file = names[name] ?: return@mapIndexedNotNull null
            PageRef(id = "$chapterId/$name", index = index, uri = file.toURI().toString())
        }
    }

    override fun open(page: PageRef): Flow<PageLoad> = flow {
        // Страница уже распакованного архива — обычный файл на диске,
        // копировать её второй раз незачем.
        if (page.uri.startsWith("file:")) {
            val file = File(java.net.URI(page.uri))
            if (file.isFile && file.length() > 0) {
                emit(PageLoad.Done(file))
            } else {
                emit(PageLoad.Error(IllegalStateException("страница не распакована: ${page.uri}")))
            }
            return@flow
        }
        val target = File(cacheDir, PageCacheKey.of(page))
        if (target.isFile && target.length() > 0) {
            // Отметка времени обязательна: каталог вытесняется по LRU от
            // lastModified, а переиспользование файла его не меняет —
            // без этого только что открытая страница выглядит самой
            // давней и уходит первой.
            target.setLastModified(System.currentTimeMillis())
            emit(PageLoad.Done(target))
            return@flow
        }
        // Пишем во временный файл в том же подкаталоге, что и сетевой
        // источник: вытеснение туда не заглядывает, и оборванная копия
        // не будет принята читалкой за готовую страницу.
        val part = File(File(cacheDir, PageCacheKey.INCOMPLETE_DIR).apply { mkdirs() }, target.name + ".part")
        try {
            context.contentResolver.openInputStream(Uri.parse(page.uri)).use { input ->
                requireNotNull(input) { "документ недоступен: ${page.uri}" }
                part.outputStream().buffered().use { out -> input.copyTo(out) }
            }
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            emit(PageLoad.Done(target))
        } catch (t: Throwable) {
            part.delete()
            emit(PageLoad.Error(t))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun nextChapter(chapterId: String): String? = neighbour(chapterId, +1)

    override suspend fun prevChapter(chapterId: String): String? = neighbour(chapterId, -1)

    private fun neighbour(chapterId: String, step: Int): String? {
        if (chapterId == TreeLibrary.FLAT_CHAPTER_ID) return null
        val index = chapterOrder.indexOf(chapterId)
        if (index < 0) return null
        return chapterOrder.getOrNull(index + step)
    }
}
