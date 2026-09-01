package dev.anime.player.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anime.player.core.Media3Engine
import dev.anime.player.skip.SkipSegment
import dev.anime.player.ui.PlayerScreen
import dev.anime.player.ui.formatTime
import dev.takami.app.ui.theme.Aurora
import java.io.File
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Экран вкладки «Аниме» — единственная точка входа модуля `:player` в приложение.
 *
 * Обязательных параметров нет: хост подключает как `AnimeScreen()`. Внутри —
 * список тайтлов и серий (локальные файлы + тестовые потоки, пока нет каталога),
 * по тапу открывается [PlayerScreen] с возобновлением с сохранённой секунды.
 *
 * Движок создаётся только когда серия реально открыта и освобождается при выходе:
 * ExoPlayer держит кодек и звуковой фокус, а вкладку могут открыть и не смотреть.
 */
@Composable
fun AnimeScreen(
    modifier: Modifier = Modifier,
    /**
     * Папка, выбранная пользователем через системный диалог (`content://`).
     * Основной способ: внутренний каталог с телефона недоступен, положить
     * туда файлы нельзя. Media3 играет документы напрямую, копирование в кеш
     * не нужно.
     */
    contentTree: Uri? = null,
    /** Запасной путь на диске; используется, когда папка не выбрана. */
    contentRoot: File? = null,
) {
    val context = LocalContext.current
    val progress = remember { WatchProgressStore(context) }
    val root = contentRoot ?: context.filesDir

    // Обход каталога — дисковый ввод-вывод, держим его вне главного потока:
    // на выбранной папке это ещё и запросы к content-провайдеру.
    var scanned by remember { mutableStateOf<List<AnimeCatalog.Title>?>(null) }
    LaunchedEffect(contentTree, root.absolutePath) {
        scanned = withContext(Dispatchers.IO) {
            val local = if (contentTree != null) {
                AnimeCatalog.scanTree(context, contentTree)
            } else {
                AnimeCatalog.scan(root)
            }
            local + AnimeCatalog.demoStreams()
        }
    }
    val titles = scanned
    var playing by remember { mutableStateOf<AnimeCatalog.Episode?>(null) }
    // Перечитываем прогресс после выхода из плеера, чтобы список сразу показал новую метку.
    var progressStamp by remember { mutableStateOf(0) }

    val episode = playing
    if (titles == null) {
        Box(modifier.fillMaxSize().background(Aurora.Surface))
    } else if (episode == null) {
        EpisodeList(
            titles = titles,
            hint = if (contentTree != null) {
                "Файлы из выбранной папки. Каталог из сети подключается отдельно."
            } else {
                "Папка не выбрана — в Настройках укажите папку с файлами. Ниже только тестовые потоки."
            },
            modifier = modifier,
            positionOf = { id -> progressStamp; progress.position(id) },
            durationOf = { id -> progress.duration(id) },
            onOpen = { playing = it },
        )
    } else {
        EpisodePlayer(
            episode = episode,
            progress = progress,
            modifier = modifier,
            onExit = {
                playing = null
                progressStamp++
            },
        )
    }
}

@Composable
private fun EpisodePlayer(
    episode: AnimeCatalog.Episode,
    progress: WatchProgressStore,
    modifier: Modifier,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val engine = remember(episode.id) { Media3Engine(context) }
    val state by engine.state.collectAsState()
    // Skip-тайминги приедут вместе с каталогом: AniSkip индексирует эпизоды по id тайтла,
    // которого у локального файла нет. Провайдер готов (:player/skip/net), подключается там же.
    val segments = remember(episode.id) { emptyList<SkipSegment>() }

    LaunchedEffect(episode.id) {
        engine.load(episode.url, startMs = progress.resumeFrom(episode.id))
    }

    // Сохраняем позицию по ходу просмотра, а не только на выходе: процесс могут
    // убить в фоне, и тогда «продолжить с той же секунды» не сработало бы.
    LaunchedEffect(episode.id, state.positionMs / 5000L) {
        progress.save(episode.id, state.positionMs, state.durationMs)
    }

    DisposableEffect(episode.id) {
        onDispose {
            progress.save(episode.id, engine.state.value.positionMs, engine.state.value.durationMs)
            engine.release()
        }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        PlayerScreen(
            engine = engine,
            segments = segments,
            autoSkip = false,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = "‹ Назад",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .clickable { onExit() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun EpisodeList(
    titles: List<AnimeCatalog.Title>,
    hint: String,
    modifier: Modifier,
    positionOf: (String) -> Long,
    durationOf: (String) -> Long,
    onOpen: (AnimeCatalog.Episode) -> Unit,
) {
    LazyColumn(
        modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 120.dp),
    ) {
        item {
            Text("Аниме", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                hint,
                color = Aurora.OnSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(18.dp))
        }
        titles.forEach { title ->
            item(key = "t-" + title.id) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        plural(title.episodeCount),
                        color = Aurora.OnSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            items(title.episodes, key = { it.id }) { ep ->
                EpisodeRow(
                    episode = ep,
                    positionMs = positionOf(ep.id),
                    durationMs = durationOf(ep.id),
                    onClick = { onOpen(ep) },
                )
            }
            item(key = "s-" + title.id) { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: AnimeCatalog.Episode,
    positionMs: Long,
    durationMs: Long,
    onClick: () -> Unit,
) {
    val label = PlaybackPosition.resumeLabel(positionMs, durationMs, ::formatTime)
    val fraction = PlaybackPosition.progressFraction(positionMs, durationMs)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Aurora.SurfaceContainer)
            .border(1.dp, Aurora.Outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Aurora.Acc.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (episode.number > 0) episode.number.toString() else "•",
                    color = Aurora.Acc2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(episode.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (label.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(label, color = Aurora.OnSurfaceVariant, fontSize = 12.sp)
                }
            }
            if (!episode.isLocal) {
                Text("сеть", color = Aurora.Acc3, fontSize = 11.sp)
            }
        }
        if (fraction > 0f) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Aurora.Outline.copy(alpha = 0.4f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Aurora.Acc),
                )
            }
        }
    }
}

private fun plural(count: Int): String {
    val n = count % 100
    val last = count % 10
    val word = when {
        n in 11..14 -> "серий"
        last == 1 -> "серия"
        last in 2..3 -> "серии"
        last == 4 -> "серии"
        else -> "серий"
    }
    return "$count $word"
}
