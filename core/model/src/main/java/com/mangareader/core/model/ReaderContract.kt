package com.mangareader.core.model

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.Flow

/**
 * The ONLY public entry point of the manga-reader component.
 *
 * Everything else in `:reader:*`, `:translate:*` and `:feature:reader` is
 * `internal` or otherwise not exported. The host application is expected
 * to depend on `:feature:reader` (which provides the DI-wired
 * implementation of this interface) and on `:core:model` for these types.
 *
 * The implementation is provided by `:feature:reader`
 * (`com.mangareader.feature.reader.MangaReaderImpl`) — see INTEGRATION.md.
 */
interface MangaReader {

    /**
     * Builds an [Intent] that launches the reader Activity for [params].
     * The host decides how to start it (startActivity, deep link, etc.).
     *
     * A Compose-navigation alternative (`readerDestination(params): NavGraphBuilder.() -> Unit`)
     * is intentionally not part of this minimal contract to keep the surface
     * small; see INTEGRATION.md §"Compose host integration" for the
     * companion extension the host can use if it navigates with Compose
     * Navigation instead of Activities.
     */
    fun open(context: Context, params: ReaderParams): Intent

    /**
     * Stream of everything the host needs to know about reading activity.
     * The reader does NOT write progress to a library database and does
     * NOT call trackers itself — see [ReaderEvent] kdoc. Collect this from
     * process-wide scope (e.g. Application-level) so events are not missed
     * if the host's screen isn't currently visible.
     */
    val events: Flow<ReaderEvent>
}

/**
 * Parameters needed to open the reader at a specific position.
 *
 * The reader is intentionally shallow here: it stores no notion of
 * "library", "source id meaning" or "series metadata" — [sourceId] is
 * opaque and is only ever forwarded back out via [ReaderEvent] or to
 * whichever [MangaPageSource] the host's DI graph has bound for it.
 */
data class ReaderParams(
    val mangaId: String,
    val chapterId: String,
    val startPage: Int = 0,
    /** Opaque; the reader never interprets this, only threads it through. */
    val sourceId: String,
)

/**
 * Everything the reader reports to the host. The host is the sole owner of
 * persisted reading progress and of any tracker/sync integration — the
 * reader is deliberately not allowed to write to a library database or
 * call a tracker API itself, because at integration time the "library"
 * concept does not exist inside this module and must not be invented here.
 */
sealed interface ReaderEvent {

    /** A page became the current/settled page in the reading viewport. */
    data class PageRead(val chapterId: String, val page: Int, val total: Int) : ReaderEvent

    /**
     * The bottom edge of a chapter's last page has scrolled above the
     * viewport's bottom edge (§5.7 — "a chapter is considered read when...").
     * In continuous/webtoon modes this fires while the feed keeps scrolling
     * seamlessly into the next chapter; it is NOT a full-screen event and
     * must not be treated as a hard boundary by the host UI.
     */
    data class ChapterCompleted(val chapterId: String) : ReaderEvent

    /** The reader's "current chapter" (used for title bar / progress) changed. */
    data class ChapterChanged(val fromId: String, val toId: String) : ReaderEvent

    /** A page's translation finished and is now renderable. */
    data class TranslationReady(val pageKey: PageKey) : ReaderEvent

    data class Failure(val kind: FailureKind, val message: String) : ReaderEvent
}

enum class FailureKind {
    PAGE_LOAD,
    CHAPTER_LOAD,
    TRANSLATION,
    UNKNOWN,
}
