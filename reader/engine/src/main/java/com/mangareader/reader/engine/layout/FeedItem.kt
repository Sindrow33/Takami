package com.mangareader.reader.engine.layout

import com.mangareader.core.model.EdgeColors
import com.mangareader.core.model.PageKey
import com.mangareader.core.model.PageRef

/**
 * One entry in the seamless cross-chapter feed (§5.7).
 *
 * The feed's list is a flat sequence of [FeedItem]s where pages of chapter
 * N and N+1 are interleaved into ONE continuous list — there is no
 * "chapter boundary" item type that renders as a visual break. A chapter
 * change is represented purely by [FeedItem.Page.chapterId] changing
 * between consecutive entries; the UI layer (per §5.7) reacts to that by
 * quietly updating the top bar and firing a haptic tick, never by
 * inserting a full-bleed "Next: Chapter 13" screen (that stays an opt-in
 * setting handled by the UI layer for paged modes only).
 */
sealed interface FeedItem {

    data class Page(
        val chapterId: String,
        val chapterNumber: Float,
        val pageRef: PageRef,
        val pageKey: PageKey? = null,
        val state: PageState = PageState.PENDING,
        /** Measured or estimated height in px at the feed's fit-width layout width. */
        val layoutHeightPx: Int,
        val isHeightEstimated: Boolean,
        val edgeColors: EdgeColors? = null,
    ) : FeedItem

    /**
     * The two legitimate empty surfaces (§5.6): top of the very first
     * chapter, bottom of the last known chapter. Rendered as a designed
     * surface (cover card / "you've caught up" state), not a void — see
     * `:reader:ui`'s `EndCapOverlay`. This item type exists so the layout
     * engine can reserve exact space for it like any other item.
     */
    data class EndCap(
        val kind: EndCapKind,
        val layoutHeightPx: Int,
    ) : FeedItem
}

enum class EndCapKind { SERIES_START, SERIES_END_CAUGHT_UP, SERIES_END_NO_MORE_CHAPTERS }

enum class PageState { PENDING, LOADING, DECODED, ERROR }
