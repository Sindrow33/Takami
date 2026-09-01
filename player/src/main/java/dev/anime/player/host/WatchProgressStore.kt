package dev.anime.player.host

import android.content.Context

/**
 * Позиция просмотра по серии. Намеренно повторяет `ReadingProgressStore`
 * читалки: SharedPreferences сейчас, общая база — потом, ключи не поменяются.
 *
 * Вся логика «с какой секунды открывать» и «стоит ли сохранять» лежит в
 * [PlaybackPosition] и покрыта тестами; здесь только чтение и запись.
 */
class WatchProgressStore(context: Context) {
    private val sp = context.getSharedPreferences("takami.anime.progress", Context.MODE_PRIVATE)

    fun save(episodeId: String, positionMs: Long, durationMs: Long) {
        if (!PlaybackPosition.shouldSave(positionMs, durationMs)) {
            if (PlaybackPosition.isWatched(positionMs, durationMs)) markWatched(episodeId, durationMs)
            return
        }
        sp.edit()
            .putLong(posKey(episodeId), positionMs)
            .putLong(durKey(episodeId), durationMs)
            .apply()
    }

    fun markWatched(episodeId: String, durationMs: Long) {
        sp.edit()
            .putLong(posKey(episodeId), durationMs)
            .putLong(durKey(episodeId), durationMs)
            .apply()
    }

    fun position(episodeId: String): Long = sp.getLong(posKey(episodeId), 0L)

    fun duration(episodeId: String): Long = sp.getLong(durKey(episodeId), 0L)

    /** С какой позиции открывать серию. */
    fun resumeFrom(episodeId: String): Long =
        PlaybackPosition.resumeFrom(position(episodeId), duration(episodeId))

    private fun posKey(id: String) = "pos:$id"
    private fun durKey(id: String) = "dur:$id"
}
