package dev.takami.app.library

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangareader.core.model.ReaderParams
import com.mangareader.feature.reader.ReaderSourceRegistry
import dev.takami.app.data.ReadingProgressStore
import dev.takami.app.ui.components.Icon
import dev.takami.app.ui.components.Pill
import dev.takami.app.ui.components.TakamiIcon
import dev.takami.app.ui.theme.Aurora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Вкладка «Библиотека»: список локальных тайтлов → главы → запуск читалки.
 *
 * Онлайн-каталог сюда встанет тем же списком, когда парсер отдаст свой
 * `MangaPageSource` — экран работает через тот же реестр источников.
 */
@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    var titles by remember { mutableStateOf<List<LocalLibrary.Title>>(emptyList()) }
    var openedTitle by remember { mutableStateOf<LocalLibrary.Title?>(null) }
    var scanned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        titles = withContext(Dispatchers.IO) { LocalLibrary.titles(context) }
        scanned = true
    }

    val title = openedTitle
    when {
        title != null -> ChapterList(title, onBack = { openedTitle = null })
        titles.isEmpty() && scanned -> EmptyLibrary(LocalLibrary.rootDir(context).absolutePath)
        else -> TitleList(titles) { openedTitle = it }
    }
}

@Composable
private fun TitleList(titles: List<LocalLibrary.Title>, onOpen: (LocalLibrary.Title) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(Aurora.Surface),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Библиотека",
                color = Aurora.OnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        items(titles) { title ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Aurora.RadiusM))
                    .background(Aurora.SurfaceContainer)
                    .clickable { onOpen(title) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title.name, color = Aurora.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${title.chapterCount} глав",
                        color = Aurora.OnSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Pill("локально", tint = Aurora.TypeManga)
            }
        }
    }
}

@Composable
private fun ChapterList(title: LocalLibrary.Title, onBack: () -> Unit) {
    val context = LocalContext.current
    val progress = remember { ReadingProgressStore(context) }
    LazyColumn(
        Modifier.fillMaxSize().background(Aurora.Surface),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text(
                    "← Библиотека",
                    color = Aurora.Acc2,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
                )
                Text(title.name, color = Aurora.OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        items(title.chapters) { chapter ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Aurora.RadiusS))
                    .background(Aurora.Sub)
                    .clickable { openReader(context, title, chapter) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(chapter.name, color = Aurora.OnSurface, fontSize = 14.sp)
                    ChapterProgressLabel(progress, chapter.id)
                }
                Text(
                    if (chapter.isArchive) "CBZ" else "папка",
                    color = Aurora.OnSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/**
 * Подпись о прогрессе главы. Прочитанная и начатая выглядят по-разному,
 * непрочитанная не занимает места — иначе список превращается в стену
 * одинаковых строк.
 */
@Composable
private fun ChapterProgressLabel(progress: ReadingProgressStore, chapterId: String) {
    val done = progress.isCompleted(chapterId)
    val page = progress.page(chapterId)
    val total = progress.total(chapterId)

    when {
        done -> Text(
            "прочитано",
            color = Aurora.Ok,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        page > 0 -> Text(
            if (total > 0) "остановились на ${page + 1} из $total" else "остановились на ${page + 1}",
            color = Aurora.Acc2,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun EmptyLibrary(path: String) {
    Column(
        Modifier.fillMaxSize().background(Aurora.Surface).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(TakamiIcon.Brain, Modifier.size(40.dp), Aurora.Acc2)
        Spacer(Modifier.height(16.dp))
        Text("Библиотека пуста", color = Aurora.OnSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Читалка работает с локальными главами: папка со страницами или CBZ. " +
                "Положите главы в $path — онлайн-источники подключатся сюда же, когда приедет парсер.",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun openReader(context: Context, title: LocalLibrary.Title, chapter: LocalLibrary.Chapter) {
    val sourceId = "${LocalLibrary.SOURCE_ID_LOCAL}:${title.id}"
    if (!ReaderSourceRegistry.isRegistered(sourceId)) {
        ReaderSourceRegistry.register(
            sourceId = sourceId,
            source = LocalLibrary.sourceFor(context, title),
            chapterLookup = LocalLibrary.chapterLookup(title),
        )
    }
    // Продолжаем с сохранённой страницы: прогресс пишется на каждой
    // странице, но до этого места не доезжал — глава всегда
    // открывалась с начала.
    val startPage = ReadingProgressStore(context).page(chapter.id)

    val intent = ReaderSourceRegistry.reader.open(
        context,
        ReaderParams(
            mangaId = title.id,
            chapterId = chapter.id,
            startPage = startPage,
            sourceId = sourceId,
        ),
    )
    context.startActivity(intent)
}
