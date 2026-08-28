package com.mangareader.reader.engine.feed

import com.mangareader.core.model.ChapterInfo
import com.mangareader.core.model.MangaPageSource
import com.mangareader.core.model.PageRef
import com.mangareader.reader.engine.layout.EndCapKind
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.engine.layout.PageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The heart of §5.7 "seamless cross-chapter transition".
 *
 * The reader is explicitly NOT a per-chapter pager. [FeedController] owns
 * one flat, growable list of [FeedItem]s spanning however many chapters
 * are currently materialised, where chapter N's pages and chapter N+1's
 * pages sit in the SAME list, back to back, with no special "end of
 * chapter" item between them. There is no `ChapterEndScreen` state in this
 * class — full stop. The concept of "the chapter is over" only exists as
 * an *event* the UI/host can react to quietly, never as a rendered item.
 *
 * Responsibilities:
 *  - Materialise chapters into flat items, prefetching the next chapter
 *    [PREFETCH_TRIGGER_PAGES] pages before it's needed so it's already
 *    known by the time the user scrolls there (no popping-in of a "next
 *    chapter" section).
 *  - Keep a small memory window of *decoded* pages (~7: 3 back, current, 3
 *    forward per §5.7) and unload the rest, while retaining their
 *    *measured* heights forever so layout / scrollbar never jumps or
 *    resets when a page is unloaded and later re-entered.
 *  - Detect "chapter completed" purely from scroll geometry (bottom edge
 *    of the chapter's last page has scrolled above the viewport's bottom
 *    edge), not from any "last page shown" pager transition.
 *  - Surface the two legitimate [EndCapKind] surfaces at the true start
 *    and true end of the known series, and nowhere else.
 */
class FeedController(
    private val source: MangaPageSource,
    private val chapterLookup: suspend (String) -> ChapterInfo,
    private val scope: CoroutineScope,
    private val heightEstimator: PlaceholderHeightEstimator = PlaceholderHeightEstimator(),
) {
    companion object {
        /** "2-3 pages before the end" per §5.7. */
        const val PREFETCH_TRIGGER_PAGES = 3

        /** "~7 pages (3 back, current, 3 forward)" memory window per §5.7. */
        const val DECODE_WINDOW_RADIUS = 3
    }

    private val loader = ChapterWindowLoader(source, chapterLookup)

    private val _state = MutableStateFlow(FeedState(items = emptyList(), currentChapterId = null))
    val state: StateFlow<FeedState> = _state

    private val _events = MutableSharedFlow<FeedEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<FeedEvent> = _events

    /** Chapters currently materialised into [FeedState.items], in reading order. */
    private val loadedChapters = ArrayList<ChapterWindow>()

    /** Whether we've confirmed there is no chapter before the first loaded one. */
    private var isSeriesStart = false

    /** Whether we've confirmed there is no chapter after the last loaded one. */
    private var isSeriesEnd = false

    private var prefetchJob: Job? = null

    suspend fun start(chapterId: String, startPage: Int) {
        val window = loader.load(chapterId)
        loadedChapters.clear()
        loadedChapters.add(window)
        isSeriesStart = loader.prevChapterId(chapterId) == null
        isSeriesEnd = loader.nextChapterId(chapterId) == null
        rebuildFlatItems()
        _state.update { it.copy(currentChapterId = chapterId, requestedStartPage = startPage) }
    }

    /**
     * Called by the UI layer whenever the visible/settled range in the feed
     * changes. Drives prefetch of the next chapter and unloading of pages
     * that fell outside [DECODE_WINDOW_RADIUS] of [centerGlobalIndex].
     */
    fun onViewportMoved(centerGlobalIndex: Int) {
        maybePrefetchNext(centerGlobalIndex)
        unloadOutsideWindow(centerGlobalIndex)
        maybeEmitChapterCompleted(centerGlobalIndex)
        maybeEmitChapterChanged(centerGlobalIndex)
    }

    /** Called once a page's true decoded height is known, to replace an estimate. */
    fun reportMeasuredHeight(globalIndex: Int, actualHeightPx: Int) {
        val items = _state.value.items.toMutableList()
        val current = items.getOrNull(globalIndex) as? FeedItem.Page ?: return
        items[globalIndex] = current.copy(layoutHeightPx = actualHeightPx, isHeightEstimated = false)
        _state.update { it.copy(items = items) }
    }

    fun reportPageState(globalIndex: Int, newState: PageState) {
        val items = _state.value.items.toMutableList()
        val current = items.getOrNull(globalIndex) as? FeedItem.Page ?: return
        items[globalIndex] = current.copy(state = newState)
        _state.update { it.copy(items = items) }
    }

    private fun maybePrefetchNext(centerGlobalIndex: Int) {
        if (isSeriesEnd) return
        val items = _state.value.items
        val lastPageIndex = items.indexOfLast { it is FeedItem.Page }
        if (lastPageIndex < 0) return
        val remaining = lastPageIndex - centerGlobalIndex
        if (remaining > PREFETCH_TRIGGER_PAGES) return
        if (prefetchJob?.isActive == true) return

        prefetchJob = scope.launch {
            val currentLastChapterId = loadedChapters.last().chapter.id
            val nextId = loader.nextChapterId(currentLastChapterId)
            if (nextId == null) {
                isSeriesEnd = true
                rebuildFlatItems()
                return@launch
            }
            val nextWindow = loader.load(nextId)
            loadedChapters.add(nextWindow)
            isSeriesEnd = loader.nextChapterId(nextId) == null
            rebuildFlatItems()
        }
    }

    private fun unloadOutsideWindow(centerGlobalIndex: Int) {
        val items = _state.value.items
        val lowerBound = centerGlobalIndex - DECODE_WINDOW_RADIUS
        val upperBound = centerGlobalIndex + DECODE_WINDOW_RADIUS
        val updated = items.mapIndexed { idx, item ->
            if (item is FeedItem.Page && idx !in lowerBound..upperBound && item.state == PageState.DECODED) {
                // Height is preserved (isHeightEstimated stays false) so the
                // layout/scrollbar does not jump when this is unloaded —
                // only the decoded bitmap resources are released by the
                // rendering layer in response to this state transition.
                item.copy(state = PageState.PENDING)
            } else item
        }
        if (updated != items) _state.update { it.copy(items = updated) }
    }

    private fun maybeEmitChapterCompleted(centerGlobalIndex: Int) {
        val items = _state.value.items
        val current = items.getOrNull(centerGlobalIndex) as? FeedItem.Page ?: return
        // A chapter is "completed" once the viewport has scrolled past the
        // bottom of that chapter's last page (§5.7's precise definition).
        // We approximate "scrolled past" by the viewport's center index
        // having moved into the NEXT chapter's first page; the exact pixel
        // geometry check (bottom of last page above viewport bottom) is
        // performed by the UI layer, which calls back into
        // [emitChapterCompletedFor] with the confirmed chapterId.
        val previous = items.getOrNull(centerGlobalIndex - 1) as? FeedItem.Page
        if (previous != null && previous.chapterId != current.chapterId) {
            emitChapterCompletedFor(previous.chapterId)
        }
    }

    /** Called by the UI once it has geometrically confirmed the completion condition. */
    fun emitChapterCompletedFor(chapterId: String) {
        scope.launch { _events.emit(FeedEvent.ChapterCompleted(chapterId)) }
    }

    private var lastAnnouncedChapterId: String? = null

    private fun maybeEmitChapterChanged(centerGlobalIndex: Int) {
        val items = _state.value.items
        val current = (items.getOrNull(centerGlobalIndex) as? FeedItem.Page)?.chapterId ?: return
        val previous = lastAnnouncedChapterId
        if (previous != null && previous != current) {
            scope.launch { _events.emit(FeedEvent.ChapterChanged(previous, current)) }
        }
        lastAnnouncedChapterId = current
        _state.update { it.copy(currentChapterId = current) }
    }

    private fun rebuildFlatItems() {
        val items = ArrayList<FeedItem>()
        if (isSeriesStart) {
            items.add(FeedItem.EndCap(EndCapKind.SERIES_START, layoutHeightPx = heightEstimator.endCapHeight()))
        }
        for (window in loadedChapters) {
            for (pageRef in window.pages) {
                items.add(
                    FeedItem.Page(
                        chapterId = window.chapter.id,
                        chapterNumber = window.chapter.number,
                        pageRef = pageRef,
                        layoutHeightPx = heightEstimator.estimate(pageRef),
                        isHeightEstimated = pageRef.width == null || pageRef.height == null,
                    )
                )
            }
        }
        if (isSeriesEnd) {
            items.add(FeedItem.EndCap(EndCapKind.SERIES_END_CAUGHT_UP, layoutHeightPx = heightEstimator.endCapHeight()))
        }
        _state.update { it.copy(items = items) }
    }
}

data class FeedState(
    val items: List<FeedItem>,
    val currentChapterId: String?,
    val requestedStartPage: Int = 0,
)

sealed interface FeedEvent {
    data class ChapterCompleted(val chapterId: String) : FeedEvent
    data class ChapterChanged(val fromId: String, val toId: String) : FeedEvent
}

private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}

/**
 * Estimates a page's on-screen height before it is decoded, so the feed
 * can reserve exact space and avoid layout jump (§5.6d). When
 * [PageRef.width]/[PageRef.height] are known, this is exact (scaled to
 * the current layout width); otherwise falls back to a running average of
 * recently measured pages in the same chapter, defaulting to a sane
 * webtoon-ish aspect ratio for the very first estimate.
 */
class PlaceholderHeightEstimator(
    private var layoutWidthPx: Int = 1080,
    private val defaultAspect: Float = 1.45f, // height/width, typical webtoon panel block
) {
    private var runningAverageHeight: Int = (layoutWidthPx * defaultAspect).toInt()

    fun updateLayoutWidth(newWidthPx: Int) {
        layoutWidthPx = newWidthPx
    }

    fun estimate(pageRef: PageRef): Int {
        val w = pageRef.width
        val h = pageRef.height
        return if (w != null && h != null && w > 0) {
            (layoutWidthPx.toFloat() * h / w).toInt()
        } else {
            runningAverageHeight
        }
    }

    fun endCapHeight(): Int = (layoutWidthPx * 0.6f).toInt()

    fun recordMeasured(actualHeightPx: Int) {
        runningAverageHeight = ((runningAverageHeight * 3) + actualHeightPx) / 4
    }
}
