package app.takami.source

import app.takami.model.Format

/** контракт парсера: расширения реализуют его, ядро о сайтах не знает */
interface Source {
    val id: String
    val name: String
    val lang: String
    val format: Format
    suspend fun search(query: String): List<SourceTitle>
    suspend fun chapters(titleId: String): List<SourceChapter>
    suspend fun pages(chapterId: String): List<String>
}

data class SourceTitle(val id: String, val name: String, val cover: String?)
data class SourceChapter(val id: String, val number: Float, val title: String?, val at: Long?)

sealed interface SourceState {
    data object Ok : SourceState
    data class Down(val untilMs: Long, val fails: Int) : SourceState
}
