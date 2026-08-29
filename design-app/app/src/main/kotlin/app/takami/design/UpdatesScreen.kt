package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Update(
    val key: String,
    val item: FakeTitle,
    val num: Int,
    val day: String,
    val time: String,
)

private fun fakeUpdates(): List<Update> {
    val days = listOf("Сегодня", "Сегодня", "Сегодня", "Вчера", "Вчера", "13 мая")
    val out = mutableListOf<Update>()
    Fake.titles.filter { !it.broken }.forEach { t ->
        val n = 2 + t.id % 3
        repeat(n) { k ->
            val num = 44 - k
            out += Update(
                key = "${t.id}-$num", item = t, num = num,
                day = days[(t.id + k) % days.size],
                time = "%02d:%02d".format(9 + (t.id * 3 + k * 5) % 13, if (k % 2 == 0) 0 else 30),
            )
        }
    }
    return out
}

@Composable
fun UpdatesScreen(onBack: () -> Unit = {}, onOpen: (FakeTitle) -> Unit = {}) {
    val all = remember { fakeUpdates() }
    val read = remember { mutableStateListOf<String>() }
    val queued = remember { mutableStateListOf<String>() }
    var filter by rememberSaveable { mutableStateOf("Всё") }

    val shown = all.filter { u ->
        when (filter) {
            "Непрочитанное" -> u.key !in read
            "Манга" -> u.item.fmt == Fmt.MANGA
            "Аниме" -> u.item.fmt == Fmt.ANIME
            "Ранобэ" -> u.item.fmt == Fmt.NOVEL
            else -> true
        }
    }
    val unread = all.count { it.key !in read }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(Dim.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹", fontSize = 24.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(horizontal = Dim.s3),
            )
            Text("Обновления", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (unread > 0) {
                Box(
                    Modifier.padding(start = 6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 1.dp)
                ) { Text("$unread", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary) }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Прочитать всё", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { all.forEach { if (it.key !in read) read += it.key } }
                    .padding(end = Dim.s4),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s3),
            horizontalArrangement = Arrangement.spacedBy(Dim.s2),
        ) {
            listOf("Всё", "Непрочитанное", "Манга", "Аниме").forEach { f ->
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
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            shown.groupBy { it.day }.forEach { (day, list) ->
                item(key = "d$day$filter") {
                    Text(
                        day.uppercase(), fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Dim.s4, Dim.s3, Dim.s4, Dim.s2),
                    )
                }
                items(list, key = { it.key }) { u ->
                    val isRead = u.key in read
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { if (!isRead) read += u.key; onOpen(u.item) }
                            .padding(Dim.s4, Dim.s2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dim.s3),
                    ) {
                        Box(
                            Modifier.width(38.dp).height(54.dp)
                                .clip(RoundedCornerShape(Dim.rS)).background(u.item.cover)
                                .graphicsAlpha(isRead)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                u.item.name, fontSize = 14.sp, maxLines = 1,
                                fontWeight = FontWeight.Medium,
                                color = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${if (u.item.fmt == Fmt.ANIME) "Эп." else "Гл."} ${u.num} · ${u.item.source} · ${u.time}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!isRead) {
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        val inQueue = u.key in queued
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(
                                    if (inQueue) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { if (inQueue) queued.remove(u.key) else queued += u.key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (inQueue) "✓" else "⤓", fontSize = 14.sp,
                                color = if (inQueue) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.graphicsAlpha(dim: Boolean) =
    if (dim) this.then(Modifier.alpha(0.45f)) else this

@Preview(name = "Updates", showBackground = true, heightDp = 800)
@Composable
private fun PreviewUpd() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { UpdatesScreen() }
}
