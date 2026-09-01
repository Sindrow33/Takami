package dev.takami.app

import android.app.Application
import com.mangareader.core.model.ReaderEvent
import com.mangareader.feature.reader.ReaderSourceRegistry
import com.mangareader.reader.engine.cache.DiskLruPageCache
import dev.takami.app.data.ReadingProgressStore
import dev.takami.app.library.ReaderPrefsStore
import dev.takami.app.settings.AppSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-scoped подписка на события читалки.
 *
 * Читалка сознательно не пишет прогресс сама (INTEGRATION.md §2) — это
 * делает приложение. Собираем именно на уровне Application, чтобы не
 * потерять событие на пересоздании активити.
 */
class TakamiApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var progress: ReadingProgressStore
        private set

    override fun onCreate() {
        super.onCreate()
        progress = ReadingProgressStore(this)
        // Настройки чтения переживают закрытие читалки: движок знает
        // только интерфейс, реализацию подставляет приложение.
        ReaderSourceRegistry.settingsStore = ReaderPrefsStore(this)
        // Кеш страниц между сессиями. Лежит в cacheDir: система сама
        // вычистит его при нехватке места, а пользовательские главы в
        // filesDir при этом не пострадают.
        ReaderSourceRegistry.diskCache = DiskLruPageCache(
            directory = java.io.File(cacheDir, "pages"),
            maxSizeBytes = AppSettingsStore(this).pageCacheLimitBytes,
        )

        appScope.launch {
            ReaderSourceRegistry.eventBus.events.collect { event ->
                when (event) {
                    is ReaderEvent.PageRead ->
                        progress.savePage(event.chapterId, event.page, event.total)
                    is ReaderEvent.ChapterCompleted ->
                        progress.markCompleted(event.chapterId)
                    is ReaderEvent.ChapterChanged,
                    is ReaderEvent.TranslationReady,
                    is ReaderEvent.Failure -> Unit
                }
            }
        }
    }
}
