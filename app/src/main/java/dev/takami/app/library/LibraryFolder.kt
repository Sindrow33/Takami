package dev.takami.app.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Папка с локальным контентом, выбранная пользователем.
 *
 * Зачем это вообще нужно: библиотека читала `filesDir`, то есть
 * приватный каталог приложения. Положить туда файлы с телефона нельзя —
 * доступа к нему у пользователя нет, — поэтому библиотека всегда была
 * пустой и читалку было не на чем проверить.
 *
 * Берём дерево через системный выбор папки (SAF): работает без
 * разрешения на всё хранилище, доступ выдаётся на конкретную папку и
 * переживает перезапуск, если его закрепить (`takePersistableUriPermission`).
 */
class LibraryFolder(private val context: Context) {

    private val prefs = context.getSharedPreferences("takami.library", Context.MODE_PRIVATE)

    var treeUri: Uri?
        get() = prefs.getString(KEY_TREE, null)?.let(Uri::parse)
        private set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_TREE) else putString(KEY_TREE, value.toString())
            }.apply()
        }

    /** Intent для системного выбора папки. */
    fun pickIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

    /**
     * Запомнить выбранную папку.
     *
     * `takePersistableUriPermission` обязателен: без него доступ живёт
     * до перезапуска процесса, и после закрытия приложения библиотека
     * снова оказалась бы пустой — с виду «настройка не сохранилась».
     */
    fun remember(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        treeUri = uri
    }

    fun forget() {
        treeUri?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        treeUri = null
    }

    /**
     * Доступна ли папка прямо сейчас.
     *
     * Проверяем не только наличие сохранённого адреса: пользователь мог
     * отозвать доступ, а карту — вынуть. Тогда честнее показать «папка
     * не выбрана», чем пустой список без объяснений.
     */
    fun isUsable(): Boolean {
        val uri = treeUri ?: return false
        val granted = context.contentResolver.persistedUriPermissions
            .any { it.uri == uri && it.isReadPermission }
        if (!granted) return false
        return DocumentFile.fromTreeUri(context, uri)?.isDirectory == true
    }

    /** Человекочитаемое имя для настроек: последний сегмент пути. */
    fun displayName(): String? {
        val uri = treeUri ?: return null
        DocumentFile.fromTreeUri(context, uri)?.name?.let { return it }
        return Uri.decode(uri.lastPathSegment)?.substringAfterLast(':')
    }

    private companion object {
        const val KEY_TREE = "treeUri"
    }
}

/**
 * Импорт выбранной папки в рабочий каталог библиотеки.
 *
 * Читалка работает с `java.io.File` — ей нужен путь, а не `content://`.
 * Поэтому содержимое выбранной папки копируется во внутренний каталог
 * один раз, а дальше библиотека читает его как раньше.
 *
 * Копируем, а не переносим: файлы пользователя мы удалять не вправе.
 */
object LibraryImport {

    /** Что нашли и что скопировали — для отчёта в интерфейсе. */
    data class Result(val titles: Int, val chapters: Int, val files: Int, val skipped: Int)

    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")
    private val ARCHIVE_EXT = setOf("cbz", "zip")

    /**
     * Ожидаемая раскладка в выбранной папке — та же, что в библиотеке:
     * `<Название>/<Глава>/0001.jpg` либо `<Название>/<Глава>.cbz`.
     *
     * Если пользователь выбрал сразу папку одного тайтла (внутри лежат
     * главы, а не тайтлы), берём её как один тайтл: так ведёт себя
     * большинство читалок, и объяснять это пользователю не нужно.
     */
    fun run(context: Context, folder: LibraryFolder): Result {
        val uri = folder.treeUri ?: return Result(0, 0, 0, 0)
        val root = DocumentFile.fromTreeUri(context, uri) ?: return Result(0, 0, 0, 0)
        val target = LocalLibrary.rootDir(context)

        var titles = 0
        var chapters = 0
        var files = 0
        var skipped = 0

        val topDirs = root.listFiles().filter { it.isDirectory }
        val looksLikeSingleTitle = topDirs.isNotEmpty() && topDirs.all { dir ->
            dir.listFiles().none { it.isDirectory }
        } && root.listFiles().none { it.isFile && it.ext() in ARCHIVE_EXT }

        val titleDirs = if (looksLikeSingleTitle) listOf(root) else topDirs

        for (titleDir in titleDirs) {
            val titleName = titleDir.name?.sanitized() ?: continue
            val titleTarget = File(target, titleName)
            var copiedInTitle = 0

            for (entry in titleDir.listFiles()) {
                when {
                    entry.isDirectory -> {
                        val chapterName = entry.name?.sanitized() ?: continue
                        val chapterTarget = File(titleTarget, chapterName)
                        var pages = 0
                        for (page in entry.listFiles()) {
                            if (!page.isFile || page.ext() !in IMAGE_EXT) { skipped++; continue }
                            if (copy(context, page, File(chapterTarget, page.name!!.sanitized()))) {
                                pages++; files++
                            } else skipped++
                        }
                        if (pages > 0) { chapters++; copiedInTitle++ }
                    }

                    entry.isFile && entry.ext() in ARCHIVE_EXT -> {
                        if (copy(context, entry, File(titleTarget, entry.name!!.sanitized()))) {
                            chapters++; files++; copiedInTitle++
                        } else skipped++
                    }

                    else -> skipped++
                }
            }

            if (copiedInTitle > 0) titles++
        }

        return Result(titles, chapters, files, skipped)
    }

    private fun copy(context: Context, from: DocumentFile, to: File): Boolean = runCatching {
        // Уже скопированный файл того же размера не переписываем: повторный
        // импорт не должен занимать место и время заново.
        if (to.isFile && to.length() == from.length() && to.length() > 0) return true

        to.parentFile?.mkdirs()
        val part = File(to.parentFile, to.name + ".part")
        context.contentResolver.openInputStream(from.uri)?.use { input ->
            part.outputStream().buffered().use { output -> input.copyTo(output) }
        } ?: return false

        // Тот же приём, что и в сетевом источнике: сначала во временный
        // файл, потом переименование. Иначе обрыв копирования оставляет
        // обрезанную страницу, которую читалка примет за целую.
        if (!part.renameTo(to)) {
            part.copyTo(to, overwrite = true)
            part.delete()
        }
        true
    }.getOrElse {
        File(to.parentFile, to.name + ".part").delete()
        false
    }

    private fun DocumentFile.ext(): String =
        name?.substringAfterLast('.', "")?.lowercase().orEmpty()

    /** Имена из чужой файловой системы не должны собирать путь наружу. */
    private fun String.sanitized(): String =
        replace(Regex("""[/\\:*?"<>|]"""), "_").trim().ifEmpty { "_" }
}
