package dev.takami.app.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mangareader.reader.engine.novel.NovelChapter

/**
 * Локальные тексты: главы ранобэ из той же выбранной папки, что и манга.
 *
 * Отдельный тип, а не флаг в [TreeLibrary], потому что различается не
 * оформление, а само содержимое: у текста нет страниц, нет разворотов и
 * нет декодирования — общего кода с изображениями почти не остаётся.
 *
 * Раскладка та же, что у манги:
 * ```
 * <папка>/<Название>/<Глава>.txt
 * ```
 * Так проверять можно уже сейчас, не дожидаясь каталога: файл кладётся
 * в ту же папку, что и манга, и появляется в библиотеке рядом.
 */
object NovelLibrary {

    private val TEXT_EXTENSIONS = setOf("txt", "text", "md")

    fun isTextChapter(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in TEXT_EXTENSIONS

    /** Тайтлы, у которых есть хотя бы одна текстовая глава. */
    fun titles(context: Context, tree: Uri): List<LocalLibrary.Title> {
        val root = DocumentFile.fromTreeUri(context, tree) ?: return emptyList()
        return root.listFiles()
            .filter { it.isDirectory }
            .sortedBy { it.name.orEmpty() }
            .mapNotNull { dir ->
                val chapters = dir.listFiles()
                    .filter { it.isFile && isTextChapter(it.name.orEmpty()) }
                    .sortedBy { it.name.orEmpty() }
                    .mapIndexed { index, file ->
                        val raw = file.name.orEmpty()
                        val name = raw.substringBeforeLast('.')
                        LocalLibrary.Chapter(
                            id = raw,
                            name = name,
                            number = LibraryNames.numberOf(name) ?: (index + 1).toFloat(),
                            isArchive = false,
                        )
                    }
                if (chapters.isEmpty()) {
                    null
                } else {
                    LocalLibrary.Title(
                        id = dir.name.orEmpty(),
                        name = dir.name.orEmpty(),
                        dir = java.io.File(dir.name.orEmpty()),
                        chapters = chapters,
                        treeUri = dir.uri,
                    )
                }
            }
    }

    /**
     * Читает главу и разбивает её на абзацы.
     *
     * Пустые строки — разделители абзацев, а не абзацы: файл, где
     * абзацы отбиты двойным переводом строки, иначе давал бы вдвое
     * больше пустых блоков, и текст расползался бы по экранам.
     */
    fun readChapter(context: Context, chapterUri: Uri, chapterId: String, title: String?): NovelChapter? {
        val text = runCatching {
            context.contentResolver.openInputStream(chapterUri)?.use { input ->
                input.bufferedReader(detectCharset(context, chapterUri)).readText()
            }
        }.getOrNull() ?: return null
        return NovelChapter(id = chapterId, title = title, paragraphs = splitParagraphs(text))
    }

    /**
     * Разбор текста на абзацы.
     *
     * Одиночный перевод строки внутри абзаца встречается в файлах,
     * свёрстанных по 80 колонок: считать его границей абзаца — значит
     * порвать каждое предложение на куски. Границей считается пустая
     * строка; одиночный перевод склеивается пробелом.
     */
    fun splitParagraphs(raw: String): List<String> {
        val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
        return normalized.split(Regex("\n[ \t]*\n+"))
            .map { block -> block.split('\n').joinToString(" ") { it.trim() }.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Кодировка файла.
     *
     * UTF-8 по умолчанию, но русские тексты из старых библиотек часто
     * лежат в windows-1251, и открытые как UTF-8 они дают сплошные
     * знаки вопроса — то есть глава «открылась», но читать её нельзя.
     * Признак: байты 0x80..0xFF, не складывающиеся в корректный UTF-8.
     */
    private fun detectCharset(context: Context, uri: Uri): java.nio.charset.Charset {
        val head = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ByteArray(DETECT_BYTES).let { buffer ->
                    val read = input.read(buffer)
                    if (read <= 0) ByteArray(0) else buffer.copyOf(read)
                }
            }
        }.getOrNull() ?: return Charsets.UTF_8
        return if (isValidUtf8(head)) Charsets.UTF_8 else CP1251

    }

    /**
     * Проверка на корректный UTF-8. Хвост буфера может обрезать
     * многобайтовый символ посередине — незавершённая в конце
     * последовательность ошибкой не считается, иначе любой русский
     * текст в UTF-8 через раз определялся бы как 1251.
     */
    fun isValidUtf8(bytes: ByteArray): Boolean {
        var index = 0
        while (index < bytes.size) {
            val byte = bytes[index].toInt() and 0xFF
            val length = when {
                byte < 0x80 -> 1
                byte in 0xC2..0xDF -> 2
                byte in 0xE0..0xEF -> 3
                byte in 0xF0..0xF4 -> 4
                else -> return false
            }
            if (index + length > bytes.size) return true // обрезанный хвост
            for (offset in 1 until length) {
                val continuation = bytes[index + offset].toInt() and 0xFF
                if (continuation !in 0x80..0xBF) return false
            }
            index += length
        }
        return true
    }

    private const val DETECT_BYTES = 4096
    private val CP1251: java.nio.charset.Charset =
        runCatching { java.nio.charset.Charset.forName("windows-1251") }.getOrDefault(Charsets.UTF_8)
}
