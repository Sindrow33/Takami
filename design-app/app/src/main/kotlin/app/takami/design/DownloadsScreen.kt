package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class TaskState { Queued, Downloading, Paused, Error, Ready }

data class Task(
    val id: Int,
    val item: FakeTitle,
    val num: Int,
    val state: TaskState,
    val percent: Int,
    val sizeMb: Int,
)

private fun seedTasks(): List<Task> {
    var id = 0
    return Fake.titles.filter { !it.broken }.flatMap { t ->
        (0 until 3).map { k ->
            val n = 40 + k
            val st = when (id) {
                0 -> TaskState.Downloading
                1, 2 -> TaskState.Queued
                3 -> TaskState.Error
                4, 5 -> TaskState.Ready
                else -> TaskState.Paused
            }
            Task(
                id = id++, item = t, num = n, state = st,
                percent = when (st) {
                    TaskState.Ready -> 100
                    TaskState.Paused -> 46
                    TaskState.Downloading -> 18
                    else -> 0
                },
                sizeMb = 40 + n * 7 % 60,
            )
        }
    }
}

@Composable
fun DownloadsScreen(onBack: () -> Unit = {}) {
    val a = LocalAurora.current
    val tasks = remember { mutableStateListOf<Task>().apply { addAll(seedTasks()) } }
    var filter by rememberSaveable { mutableStateOf("Все") }
    val capMb = 2048

    LaunchedEffect(Unit) {
        while (true) {
            delay(700)
            val i = tasks.indexOfFirst { it.state == TaskState.Downloading }
            if (i >= 0) {
                val t = tasks[i]
                val p = t.percent + 7
                tasks[i] = if (p >= 100) t.copy(state = TaskState.Ready, percent = 100)
                else t.copy(percent = p)
            } else {
                val q = tasks.indexOfFirst { it.state == TaskState.Queued }
                if (q >= 0) tasks[q] = tasks[q].copy(state = TaskState.Downloading)
            }
        }
    }

    val used = tasks.filter { it.state == TaskState.Ready }.sumOf { it.sizeMb }
    val shown = tasks.filter {
        when (filter) {
            "Активные" -> it.state in setOf(TaskState.Downloading, TaskState.Queued, TaskState.Paused)
            "Готово" -> it.state == TaskState.Ready
            "Ошибки" -> it.state == TaskState.Error
            else -> true
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(Dim.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹", fontSize = 24.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(horizontal = Dim.s3),
            )
            Text("Загрузки", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.padding(Dim.s4, 0.dp, Dim.s4, Dim.s3)) {
            Box(
                Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth((used.toFloat() / capMb).coerceIn(0f, 1f))
                        .background(
                            if (used > capMb * 0.9) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
            Text(
                "$used МБ из $capMb МБ · ${tasks.count { it.state == TaskState.Ready }} готово",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s3),
            horizontalArrangement = Arrangement.spacedBy(Dim.s2),
        ) {
            listOf("Все", "Активные", "Готово", "Ошибки").forEach { f ->
                val on = f == filter
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .background(
                            if (on) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { filter = f }
                        .padding(horizontal = Dim.s3, vertical = 6.dp)
                ) {
                    Text(
                        f, fontSize = 12.sp,
                        color = if (on) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = Dim.s4)) {
            items(shown, key = { it.id }) { t ->
                TaskRow(t, a.warn) { act ->
                    val i = tasks.indexOfFirst { it.id == t.id }
                    if (i < 0) return@TaskRow
                    tasks[i] = when (act) {
                        "pause" -> tasks[i].copy(state = TaskState.Paused)
                        "resume" -> tasks[i].copy(state = TaskState.Downloading)
                        "retry" -> tasks[i].copy(state = TaskState.Queued, percent = 0)
                        else -> tasks[i]
                    }
                    if (act == "delete") tasks.removeAt(i)
                }
            }
        }
        Button(
            onClick = {
                val base = (tasks.maxOfOrNull { it.num } ?: 40) + 1
                repeat(5) { k ->
                    tasks.add(
                        Task(
                            id = (tasks.maxOfOrNull { it.id } ?: 0) + 1 + k,
                            item = Fake.titles.first(), num = base + k,
                            state = TaskState.Queued, percent = 0,
                            sizeMb = 40 + (base + k) * 7 % 60,
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(Dim.s4).height(46.dp),
            shape = RoundedCornerShape(999.dp),
        ) { Text("Скачать ещё 5 глав", fontSize = 13.sp) }
    }
}

@Composable
private fun TaskRow(t: Task, warn: androidx.compose.ui.graphics.Color, onAct: (String) -> Unit) {
    val (label, color) = when (t.state) {
        TaskState.Queued -> "в очереди" to MaterialTheme.colorScheme.onSurfaceVariant
        TaskState.Downloading -> "скачивается · ${t.percent}%" to MaterialTheme.colorScheme.primary
        TaskState.Paused -> "пауза" to warn
        TaskState.Error -> "ошибка сети" to MaterialTheme.colorScheme.error
        TaskState.Ready -> "готово · ${t.sizeMb} МБ" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val (glyph, act) = when (t.state) {
        TaskState.Error -> "↻" to "retry"
        TaskState.Ready -> "✕" to "delete"
        TaskState.Paused -> "▶" to "resume"
        else -> "⏸" to "pause"
    }
    Row(
        Modifier.fillMaxWidth().padding(Dim.s4, Dim.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dim.s3),
    ) {
        Box(
            Modifier.width(40.dp).height(56.dp)
                .clip(RoundedCornerShape(Dim.rS)).background(t.item.cover)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.item.name, fontSize = 14.sp, maxLines = 1, fontWeight = FontWeight.Medium)
            Text(
                "${if (t.item.fmt == Fmt.ANIME) "Эп." else "Гл."} ${t.num} · ${t.item.source}",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProgressLine(t.percent, Modifier.fillMaxWidth())
            Text(label, fontSize = 11.sp, color = color)
        }
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onAct(act) },
            contentAlignment = Alignment.Center,
        ) { Text(glyph, fontSize = 15.sp) }
    }
}

@Preview(name = "Downloads", showBackground = true, heightDp = 800)
@Composable
private fun PreviewDl() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { DownloadsScreen() }
}
