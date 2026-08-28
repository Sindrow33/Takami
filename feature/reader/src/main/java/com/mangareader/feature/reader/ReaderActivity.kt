package com.mangareader.feature.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

/**
 * The reader's host Activity. Deliberately minimal here: real wiring
 * (DI-provided [com.mangareader.reader.engine.feed.FeedController],
 * [com.mangareader.translate.core.TranslationOrchestrator], the
 * repositories from `:translate:core`/`:translate:mt`) happens in
 * `ReaderViewModel` (constructed via whatever DI the host app uses — see
 * INTEGRATION.md "Wiring the DI graph"), not inlined into this Activity.
 *
 * Sets the immersive full-screen presentation required by §5.1 ("no
 * distance between the reader and the page — nothing on screen but the
 * page by default") before any content is composed, to avoid a visible
 * system-bar flash on entry.
 */
class ReaderActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MANGA_ID = "manga_id"
        const val EXTRA_CHAPTER_ID = "chapter_id"
        const val EXTRA_START_PAGE = "start_page"
        const val EXTRA_SOURCE_ID = "source_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()
        setContent {
            // ReaderRoot(mangaId, chapterId, startPage, sourceId) — composes
            // com.mangareader.reader.ui.compose.WebtoonReaderScreen (or the
            // paged equivalent) hoisted from ReaderViewModel state.
        }
    }

    private fun applyImmersiveMode() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
