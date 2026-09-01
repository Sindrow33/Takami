package com.mangareader.feature.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import dev.takami.app.ui.theme.TakamiTheme

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

        fun intent(
            context: Context,
            mangaId: String,
            chapterId: String,
            startPage: Int,
            sourceId: String,
        ): Intent = Intent(context, ReaderActivity::class.java).apply {
            putExtra(EXTRA_MANGA_ID, mangaId)
            putExtra(EXTRA_CHAPTER_ID, chapterId)
            putExtra(EXTRA_START_PAGE, startPage)
            putExtra(EXTRA_SOURCE_ID, sourceId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()
        val mangaId = intent.getStringExtra(EXTRA_MANGA_ID).orEmpty()
        val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID).orEmpty()
        val startPage = intent.getIntExtra(EXTRA_START_PAGE, 0)
        val sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()

        val source = ReaderSourceRegistry.source(sourceId)
        val chapterLookup = ReaderSourceRegistry.chapterLookup(sourceId)
        if (source == null || chapterLookup == null || chapterId.isEmpty()) {
            // Источник для этого sourceId не зарегистрирован — открывать
            // нечего; хост обязан вызвать ReaderSourceRegistry.register до
            // MangaReader.open (INTEGRATION.md §6).
            finish()
            return
        }

        val viewModel = ViewModelProvider(
            this,
            ReaderViewModel.Factory(
                source = source,
                chapterLookup = chapterLookup,
                eventBus = ReaderSourceRegistry.eventBus,
                settingsStore = ReaderSourceRegistry.settingsStore,
                seriesId = mangaId,
            ),
        )[ReaderViewModel::class.java]
        viewModel.open(chapterId, startPage)

        setContent {
            TakamiTheme {
                ReaderRoot(viewModel, Modifier.fillMaxSize())
            }
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
