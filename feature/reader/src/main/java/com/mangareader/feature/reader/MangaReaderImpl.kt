package com.mangareader.feature.reader

import android.content.Context
import android.content.Intent
import com.mangareader.core.model.MangaReader
import com.mangareader.core.model.ReaderEvent
import com.mangareader.core.model.ReaderParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The sole public implementation of [MangaReader] (see ARCHITECTURE.md
 * "Public contract"). The host application obtains this via whatever DI
 * mechanism it uses (Hilt/Koin/manual) — this class itself takes its
 * dependencies as plain constructor parameters so it stays agnostic of
 * which DI framework the eventual merged app uses.
 *
 * [events] is backed by a process-scoped [MutableSharedFlow] fed by
 * [ReaderActivity]/`ReaderViewModel` (via [ReaderEventBus]) so it keeps
 * emitting even if the reader Activity is finished and recreated —
 * required because the host, not this module, is the source of truth for
 * reading progress (§2.3) and must not lose an event to an Activity
 * lifecycle edge.
 */
class MangaReaderImpl(
    private val eventBus: ReaderEventBus,
) : MangaReader {

    override fun open(context: Context, params: ReaderParams): Intent =
        Intent(context, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_MANGA_ID, params.mangaId)
            putExtra(ReaderActivity.EXTRA_CHAPTER_ID, params.chapterId)
            putExtra(ReaderActivity.EXTRA_START_PAGE, params.startPage)
            putExtra(ReaderActivity.EXTRA_SOURCE_ID, params.sourceId)
        }

    override val events: SharedFlow<ReaderEvent> = eventBus.events
}

/**
 * Process-wide event bus. `ReaderActivity`/`ReaderViewModel` call [emit];
 * [MangaReaderImpl.events] exposes the read side. Kept as its own class
 * (rather than a bare `MutableSharedFlow` inline in the impl) so it can
 * be provided as a singleton by the host's DI graph independent of
 * [MangaReaderImpl]'s own lifecycle.
 */
class ReaderEventBus {
    private val _events = MutableSharedFlow<ReaderEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<ReaderEvent> = _events.asSharedFlow()

    suspend fun emit(event: ReaderEvent) {
        _events.emit(event)
    }
}
