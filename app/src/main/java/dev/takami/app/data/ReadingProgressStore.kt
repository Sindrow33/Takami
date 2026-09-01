package dev.takami.app.data

import android.content.Context

/**
 * Прогресс чтения — ответственность приложения, а не читалки.
 * Пока SharedPreferences: переезд на Room/DataStore — вместе с общей
 * библиотечной базой, ключи менять не придётся.
 */
class ReadingProgressStore(context: Context) {
    private val sp = context.getSharedPreferences("takami.progress", Context.MODE_PRIVATE)

    fun savePage(chapterId: String, page: Int, total: Int) {
        sp.edit()
            .putInt(pageKey(chapterId), page)
            .putInt(totalKey(chapterId), total)
            .apply()
    }

    fun markCompleted(chapterId: String) {
        sp.edit().putBoolean(doneKey(chapterId), true).apply()
    }

    fun page(chapterId: String): Int = sp.getInt(pageKey(chapterId), 0)

    fun total(chapterId: String): Int = sp.getInt(totalKey(chapterId), 0)

    fun isCompleted(chapterId: String): Boolean = sp.getBoolean(doneKey(chapterId), false)

    /**
     * Позиция в текстовой главе — в символах.
     *
     * Отдельно от страницы намеренно: страница манги и смещение в
     * тексте это разные величины, и общий ключ означал бы, что открытая
     * когда-то как манга глава уносит читателя текста в случайное
     * место.
     */
    fun saveCharOffset(chapterId: String, offset: Int, totalChars: Int) {
        sp.edit()
            .putInt(charKey(chapterId), offset)
            .putInt(charTotalKey(chapterId), totalChars)
            .apply()
    }

    fun charOffset(chapterId: String): Int = sp.getInt(charKey(chapterId), 0)

    fun charTotal(chapterId: String): Int = sp.getInt(charTotalKey(chapterId), 0)

    private fun charKey(chapterId: String) = "char:$chapterId"
    private fun charTotalKey(chapterId: String) = "chartotal:$chapterId"

    private fun pageKey(chapterId: String) = "page:$chapterId"
    private fun totalKey(chapterId: String) = "total:$chapterId"
    private fun doneKey(chapterId: String) = "done:$chapterId"
}
