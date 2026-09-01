package dev.takami.app.library

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
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
/** Что показывает раздел библиотеки. */
enum class LibraryContent { Manga, Novel }

@Composable
fun LibraryScreen(content: LibraryContent = LibraryContent.Manga) {
    val context = LocalContext.current
    val root = remember { LibraryRoot(context) }
    var titles by remember { mutableStateOf<List<LocalLibrary.Title>>(emptyList()) }
    var openedTitle by remember { mutableStateOf<LocalLibrary.Title?>(null) }
    var scanned by remember { mutableStateOf(false) }
    var rescan by remember { mutableStateOf(0) }
    // Открытая текстовая глава. Ранобэ показывается своим экраном:
    // у текста нет страниц, декодирования и разворотов, зато есть
    // перевёрстка при каждой смене настроек.
    var openedNovel by remember { mutableStateOf<OpenNovel?>(null) }

    /*
     * Выбор папки через системный диалог. Пока его не было, корнем
     * оставался внутренний каталог приложения — путь, в который
     * пользователь с телефона положить ничего не может, поэтому
     * библиотека была пуста при любом содержимом устройства.
     */
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            root.select(uri)
            scanned = false
            rescan++
        }
    }

    LaunchedEffect(rescan, content) {
        titles = withContext(Dispatchers.IO) {
            when (content) {
                LibraryContent.Manga -> LocalLibrary.allTitles(context)
                LibraryContent.Novel ->
                    root.selectedTree()?.let { NovelLibrary.titles(context, it) }.orEmpty()
            }
        }
        scanned = true
    }

    val novel = openedNovel
    if (novel != null) {
        NovelReaderHost(chapterTitle = novel.title, open = novel) { openedNovel = null }
        return
    }

    val title = openedTitle
    when {
        title != null -> ChapterList(
            title = title,
            onBack = { openedTitle = null },
            onOpenNovel = { openedNovel = it },
        )
        titles.isEmpty() && scanned -> EmptyLibrary(
            path = root.displayPath(),
            folderChosen = root.selectedTree() != null,
            selectionLost = root.hasStaleSelection(),
            content = content,
            onPick = { picker.launch(null) },
        )
        else -> TitleList(
            titles = titles,
            path = root.displayPath(),
            onPick = { picker.launch(null) },
            onOpen = { openedTitle = it },
        )
    }
}

@Composable
private fun TitleList(
    titles: List<LocalLibrary.Title>,
    path: String,
    onPick: () -> Unit,
    onOpen: (LocalLibrary.Title) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().background(Aurora.Surface),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text(
                    "Библиотека",
                    color = Aurora.OnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                // Откуда читаем — видно всегда, и сменить можно отсюда
                // же: иначе поменять папку получится, только опустошив
                // библиотеку.
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        path,
                        color = Aurora.OnSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        "сменить",
                        color = Aurora.Acc2,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable(onClick = onPick).padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                    )
                }
            }
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
private fun ChapterList(
    title: LocalLibrary.Title,
    onBack: () -> Unit,
    onOpenNovel: (OpenNovel) -> Unit,
) {
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
                    .clickable {
                        if (NovelLibrary.isTextChapter(chapter.id)) {
                            onOpenNovel(OpenNovel(title, chapter))
                        } else {
                            openReader(context, title, chapter)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(chapter.name, color = Aurora.OnSurface, fontSize = 14.sp)
                    ChapterProgressLabel(progress, chapter.id)
                }
                Text(
                    when {
                        NovelLibrary.isTextChapter(chapter.id) -> "текст"
                        chapter.isArchive -> "CBZ"
                        else -> "папка"
                    },
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
    path: String,
    folderChosen: Boolean,
    selectionLost: Boolean,
    content: LibraryContent,
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
            when {
                selectionLost ->
                    "Выбранная папка больше недоступна — её удалили, извлекли карту или отозвали доступ. " +
                        "Выберите папку заново."
                folderChosen && content == LibraryContent.Novel ->
                    "В папке «$path» текстовых глав не нашлось. Ожидаемая раскладка: " +
                        "Название/Глава.txt (также .md)."
                folderChosen ->
                    "В папке «$path» глав не нашлось. Ожидаемая раскладка: " +
                        "Название/Глава/0001.jpg или Название/Глава.cbz."
                else ->
                    "Читалка работает с локальными главами: папка со страницами или CBZ. " +
                        "Сейчас используется внутренняя папка приложения ($path) — положить туда файлы с телефона нельзя, " +
                        "поэтому выберите свою папку."
            },
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            if (folderChosen) "Выбрать другую папку" else "Выбрать папку с мангой",
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(Aurora.RadiusS))
                .background(Aurora.Acc2)
                .clickable(onClick = onPick)
                .padding(horizontal = 22.dp, vertical = 14.dp),
        )
    }
}

private fun openReader(context: Context, title: LocalLibrary.Title, chapter: LocalLibrary.Chapter) {
    val sourceId = "${LocalLibrary.SOURCE_ID_LOCAL}:${title.id}"
    if (!ReaderSourceRegistry.isRegistered(sourceId)) {
        ReaderSourceRegistry.register(
            sourceId = sourceId,
            source = LocalLibrary.anySourceFor(context, title),
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
