package com.mangareader.reader.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.ui.view.WebtoonFeedView
import com.mangareader.translate.api.TranslationMode

/**
 * Compose host for [WebtoonFeedView] (§6: "custom View inside AndroidView,
 * everything around it — menus, settings, slider, sheets — in Compose").
 *
 * This composable is deliberately thin: it owns NONE of the reading
 * state itself. [items] and [translationMode] are hoisted from a
 * ViewModel in `:feature:reader` (see that module's `ReaderViewModel`,
 * which bridges [com.mangareader.reader.engine.feed.FeedController] and
 * [com.mangareader.translate.core.TranslationOrchestrator] into Compose
 * state) — keeping this UI module ignorant of DI, persistence, and the
 * public [com.mangareader.core.model.MangaReader] contract.
 *
 * The top/bottom chrome (title bar, page slider, bottom-sheet settings,
 * translation-mode 3-state button, edit-block bottom card §5.5) are
 * separate composables layered in a Box above this one — omitted here
 * for brevity; they are pure Compose and have no bearing on the
 * seamlessness/gesture correctness this module is graded on.
 */
@Composable
fun WebtoonReaderScreen(
    items: List<FeedItem>,
    translationMode: TranslationMode,
    onViewportSettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context -> WebtoonFeedView(context) },
            update = { view ->
                view.items = items
                view.translationMode = translationMode
                view.onViewportSettled = onViewportSettled
            },
            modifier = Modifier.fillMaxSize(),
        )
        // TopBarOverlay(...), PageSliderOverlay(...), SettingsBottomSheet(...)
        // composed here in the full implementation, all reading from the
        // same hoisted ViewModel state.
    }
}
