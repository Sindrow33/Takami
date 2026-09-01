package dev.takami.app.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

/**
 * Где лежит локальный контент.
 *
 * По умолчанию это внутренний каталог приложения — путь, в который
 * пользователь с телефона положить ничего не может: файловые менеджеры
 * туда не пускают, и библиотека остаётся пустой навсегда. Поэтому корень
 * выбирается: пользователь указывает свою папку через системный
 * диалог, приложение удерживает на неё постоянное разрешение и с этого
 * момента читает главы оттуда.
 *
 * Внутренний каталог остаётся запасным вариантом: пока папка не
 * выбрана, всё работает как раньше, а сохранённый выбор может стать
 * недействительным (карту вынули, папку удалили, разрешение отозвали) —
 * тогда возвращаемся к нему же, а не падаем.
 */
class LibraryRoot(private val context: Context) {

    private val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Каталог приложения. Всегда существует, но недоступен пользователю. */
    fun internalDir(): File = File(context.filesDir, "manga").apply { mkdirs() }

    /**
     * Выбранная пользователем папка, или null.
     *
     * Проверяется не только наличие строки в настройках: сохранённый
     * URI переживает и удаление папки, и отзыв разрешения, и извлечение
     * карты. Строка, за которой ничего нет, хуже её отсутствия — с ней
     * библиотека молча пуста, вместо того чтобы предложить выбрать
     * папку заново.
     */
    fun selectedTree(): Uri? {
        val raw = sp.getString(KEY_TREE, null) ?: return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        return if (isUsable(uri)) uri else null
    }

    /** Есть ли сохранённый выбор, пусть даже уже недействительный. */
    fun hasStaleSelection(): Boolean =
        sp.getString(KEY_TREE, null) != null && selectedTree() == null

    /**
     * Запоминает выбранную папку и удерживает доступ к ней между
     * запусками. Без `takePersistableUriPermission` разрешение живёт до
     * перезапуска процесса, и наутро библиотека снова пуста.
     */
    fun select(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        sp.edit().putString(KEY_TREE, uri.toString()).apply()
    }

    /** Возврат к внутреннему каталогу; удерживаемое разрешение отдаём. */
    fun clear() {
        selectedTreeRaw()?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        sp.edit().remove(KEY_TREE).apply()
    }

    /** Человекочитаемый путь для экрана настроек. */
    fun displayPath(): String =
        selectedTree()?.let { DocumentPaths.readable(it) } ?: internalDir().absolutePath

    private fun selectedTreeRaw(): Uri? =
        sp.getString(KEY_TREE, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    private fun isUsable(uri: Uri): Boolean {
        val held = context.contentResolver.persistedUriPermissions
            .any { it.uri == uri && it.isReadPermission }
        if (!held) return false
        return runCatching {
            androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)?.isDirectory == true
        }.getOrDefault(false)
    }

    private companion object {
        const val PREFS = "takami.library"
        const val KEY_TREE = "contentTreeUri"
    }
}

/**
 * Разбор `content://`-путей в человекочитаемый вид.
 *
 * Вынесено чистой функцией над строкой намеренно: в JVM-тестах
 * `Uri`/`DocumentFile` — заглушки, падающие с «Stub!», и логика,
 * написанная поверх них, непроверяема. Здесь на вход строка, на выход
 * строка — и правило «внешняя карта показывается не как primary»
 * проверяется тестом, а не глазами на устройстве.
 */
object DocumentPaths {

    fun readable(uri: Uri): String = readable(uri.toString())

    /**
     * `content://com.android.externalstorage.documents/tree/primary%3AManga`
     * → `Внутренняя память/Manga`.
     */
    fun readable(raw: String): String {
        val encoded = raw.substringAfterLast("/tree/", "").substringBefore("/document/")
        if (encoded.isEmpty()) return raw
        val decoded = decode(encoded)
        val volume = decoded.substringBefore(':', missingDelimiterValue = "")
        val path = decoded.substringAfter(':', missingDelimiterValue = decoded)
        val volumeName = when {
            volume.isEmpty() -> return path
            volume == "primary" -> "Внутренняя память"
            // Идентификатор карты вида 1A2B-3C4D — показывать его как
            // есть бесполезно, пользователь знает её как «SD-карту».
            volume.matches(Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")) -> "SD-карта"
            else -> volume
        }
        return if (path.isEmpty()) volumeName else "$volumeName/$path"
    }

    private fun decode(value: String): String =
        runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
