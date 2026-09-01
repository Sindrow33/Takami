package dev.takami.app.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.mangareader.reader.engine.novel.NovelChapter
import com.mangareader.reader.engine.novel.NovelPosition
import dev.takami.app.data.ReadingProgressStore
import dev.takami.app.ui.theme.Aurora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Что открыто: тайтл и его текстовая глава. */
data class OpenNovel(
    val title: LocalLibrary.Title,
    val chapter: LocalLibrary.Chapter,
)

/**
 * Загрузка текстовой главы и подключение к ней хранилищ.
 *
 * Отдельно от самого экрана чтения, чтобы экран оставался чистой
 * функцией от главы и настроек: его можно показать на любом тексте, а
 * весь ввод-вывод сосредоточен здесь.
 */
@Composable
fun NovelReaderHost(
    chapterTitle: LocalLibrary.Title,
    open: OpenNovel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val progressStore = remember { ReadingProgressStore(context) }
    val settingsStore = remember { NovelSettingsStore(context) }

    var settings by remember { mutableStateOf(settingsStore.load()) }
    var chapter by remember { mutableStateOf<NovelChapter?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(open.chapter.id) {
        chapter = withContext(Dispatchers.IO) {
            val titleDir = open.title.treeUri
                ?.let { DocumentFile.fromTreeUri(context, it) }
                ?: return@withContext null
            val file = titleDir.listFiles().firstOrNull { it.name == open.chapter.id }
                ?: return@withContext null
            NovelLibrary.readChapter(
                context = context,
                chapterUri = file.uri,
                chapterId = open.chapter.id,
                title = open.chapter.name,
            )
        }
        failed = chapter == null
    }

    val loaded = chapter
    when {
        loaded != null -> NovelReaderScreen(
            chapter = loaded,
            settings = settings,
            initialPosition = NovelPosition(progressStore.charOffset(open.chapter.id))
                .coerceIn(loaded.totalChars),
            onPositionChange = { position ->
                progressStore.saveCharOffset(open.chapter.id, position.charOffset, loaded.totalChars)
                // Дочитанной глава считается почти в конце, а не ровно
                // в конце: последний экран часто не долистывают до
                // символа, и глава оставалась бы вечно незавершённой.
                if (NovelPosition.progress(position.charOffset, loaded.totalChars) >= COMPLETION_THRESHOLD) {
                    progressStore.markCompleted(open.chapter.id)
                }
            },
            onSettingsChange = { updated ->
                settings = updated
                settingsStore.save(updated)
            },
            onBack = onBack,
        )

        failed -> Box(Modifier.fillMaxSize().background(Aurora.Surface)) {
            Text(
                "Главу не удалось прочитать. Проверьте, что файл на месте и доступ к папке не отозван.",
                color = Aurora.OnSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }

        else -> Box(Modifier.fillMaxSize().background(Aurora.Surface)) {
            Text(
                "Открываем главу…",
                color = Aurora.OnSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private const val COMPLETION_THRESHOLD = 0.98f
