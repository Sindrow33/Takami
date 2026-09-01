package core.model  // ParsedPayload — переехал сюда из core/engine

import kotlinx.serialization.Serializable

@Serializable
sealed interface ParsedPayload {
    @Serializable data class Listing(
        val items: List<MediaItem>,
        val nextPage: String? = null,
    ) : ParsedPayload

    @Serializable data class Entry(val entry: MediaEntry) : ParsedPayload
    @Serializable data class Units(val units: List<MediaUnit>) : ParsedPayload
    @Serializable data class Content(val content: TerminalContent) : ParsedPayload

    val size: Int get() = when (this) {
        is Listing -> items.size
        is Entry -> 1
        is Units -> units.size
        is Content -> content.size
    }

    /** Плоский доступ к колонкам — на этом работает и валидация, и прототипы. */
    fun bearers(): List<FieldBearing> = when (this) {
        is Listing -> items
        is Entry -> listOf(entry)
        is Units -> units
        is Content -> emptyList()
    }
}
