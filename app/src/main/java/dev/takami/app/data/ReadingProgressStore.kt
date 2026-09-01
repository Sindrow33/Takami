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

    private fun pageKey(chapterId: String) = "page:$chapterId"
    private fun totalKey(chapterId: String) = "total:$chapterId"
    private fun doneKey(chapterId: String) = "done:$chapterId"
}
