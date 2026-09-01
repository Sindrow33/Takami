package dev.takami.app.library

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.launch
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
    val folder = remember { LibraryFolder(context) }

    var titles by remember { mutableStateOf<List<LocalLibrary.Title>>(emptyList()) }
    var openedTitle by remember { mutableStateOf<LocalLibrary.Title?>(null) }
    var scanned by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<String?>(null) }
    var rescan by remember { mutableStateOf(0) }

    LaunchedEffect(rescan) {
        /*
         * Папку можно выбрать и в настройках — тогда импорт ещё не
         * запускался. Прогоняем его при открытии вкладки: повторное
         * копирование дешёвое, уже перенесённые файлы пропускаются по
         * размеру.
         */
        if (folder.isUsable()) {
            withContext(Dispatchers.IO) { LibraryImport.run(context, folder) }
        }
        titles = withContext(Dispatchers.IO) { LocalLibrary.titles(context) }
        scanned = true
    }

    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        folder.remember(uri)
        importing = true
        scope.launch {
            val res = withContext(Dispatchers.IO) { LibraryImport.run(context, folder) }
            importing = false
            report = when {
                res.files == 0 ->
                    "В выбранной папке не нашлось глав. Ожидается «Название/Глава/0001.jpg» или «Название/Глава.cbz»."
                res.skipped > 0 ->
                    "Добавлено: тайтлов ${res.titles}, глав ${res.chapters}. Пропущено файлов: ${res.skipped}."
                else -> "Добавлено: тайтлов ${res.titles}, глав ${res.chapters}."
            }
            rescan++
        }
    }

    val title = openedTitle
    when {
        title != null -> ChapterList(title, onBack = { openedTitle = null })
        titles.isEmpty() && scanned -> EmptyLibrary(
            folderName = folder.displayName().takeIf { folder.isUsable() },
            importing = importing,
            report = report,
            onPick = { pick.launch(folder.pickIntent()) },
        )
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
private fun EmptyLibrary(
    folderName: String?,
    importing: Boolean,
    report: String?,
    onPick: () -> Unit,
) {
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
            report ?: if (folderName != null) {
                "Папка «$folderName» выбрана, но глав в ней не нашлось. " +
                    "Ожидается «Название/Глава/0001.jpg» или «Название/Глава.cbz»."
            } else {
                "Выберите папку с главами на телефоне или карте памяти: " +
                    "«Название/Глава/0001.jpg» либо «Название/Глава.cbz». " +
                    "Онлайн-источники подключатся сюда же."
            },
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(Aurora.RadiusFull))
                .background(Aurora.AccentGradient)
                .clickable(enabled = !importing, onClick = onPick)
                .padding(horizontal = 22.dp, vertical = 13.dp)
        ) {
            Text(
                if (importing) "Копирую главы…"
                else if (folderName != null) "Выбрать другую папку"
                else "Выбрать папку",
                color = Aurora.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
