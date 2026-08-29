package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.util.Calendar

enum class RelState { Out, Soon, Late, Hiatus }

data class Release(
    val item: FakeTitle,
    val num: Int,
    val time: String,
    val state: RelState,
)

private fun hash(s: String): Int {
    var h = 2166136261u
    s.forEach { h = (h xor it.code.toUInt()) * 16777619u }
    return (h and 0x7FFFFFFFu).toInt()
}

/** Расписание выводится из id тайтла — детерминированно, без сети. */
private fun schedule(dayOffset: Int): List<Release> = Fake.titles.mapNotNull { t ->
    val seed = hash("${t.id}:$dayOffset")
    if (seed % 100 < 42) return@mapNotNull null
    val hour = 10 + seed / 7 % 12
    val minute = if (seed % 2 == 0) 0 else 30
    val state = when {
        t.broken -> RelState.Hiatus
        dayOffset < 0 -> RelState.Out
        dayOffset == 0 && seed % 5 == 0 -> RelState.Late
        dayOffset == 0 -> RelState.Out
        else -> RelState.Soon
    }
    Release(t, 40 + seed % 80, "%02d:%02d".format(hour, minute), state)
}

private fun dayLabel(offset: Int): Pair<String, String> {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, offset)
    val wd = listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб")[c.get(Calendar.DAY_OF_WEEK) - 1]
    return wd to c.get(Calendar.DAY_OF_MONTH).toString()
}

@Composable
fun CalendarScreen(onOpen: (FakeTitle) -> Unit = {}) {
    var day by rememberSaveable { mutableIntStateOf(0) }
    val list = schedule(day)

    Column(Modifier.fillMaxSize()) {
        Text(
            "Календарь", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(Dim.s4, Dim.s3),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dim.s4),
            horizontalArrangement = Arrangement.spacedBy(Dim.s2),
            modifier = Modifier.padding(bottom = Dim.s3),
        ) {
            items((-2..4).toList()) { off ->
                val (wd, num) = dayLabel(off)
                val on = off == day
                val count = schedule(off).size
                Column(
                    Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(Dim.rM))
                        .background(
                            if (on) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { day = off }
                        .padding(vertical = Dim.s2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        wd, fontSize = 10.sp,
                        color = if (on) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        num, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        color = if (on) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (count > 0) "$count" else "·", fontSize = 10.sp,
                        color = if (on) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(Dim.s8), contentAlignment = Alignment.Center) {
                Text(
                    "В этот день ничего не выходит",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(list, key = { it.item.id }) { r -> ReleaseRow(r) { onOpen(r.item) } }
            }
        }
    }
}

@Composable
private fun ReleaseRow(r: Release, onClick: () -> Unit) {
    val a = LocalAurora.current
    val (label, color) = when (r.state) {
        RelState.Out -> "вышло" to MaterialTheme.colorScheme.onSurfaceVariant
        RelState.Soon -> "ожидается" to MaterialTheme.colorScheme.primary
        RelState.Late -> "задержка" to a.warn
        RelState.Hiatus -> "хиатус" to MaterialTheme.colorScheme.error
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Dim.s4, Dim.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dim.s3),
    ) {
        Box(
            Modifier.width(40.dp).height(58.dp)
                .clip(RoundedCornerShape(Dim.rS)).background(r.item.cover)
        )
        Column(Modifier.weight(1f)) {
            Text(
                r.item.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                "${if (r.item.fmt == Fmt.ANIME) "Эп." else "Гл."} ${r.num} · ${r.time} · ${r.item.source}",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier.clip(RoundedCornerShape(Dim.rS))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) { Text(label, fontSize = 10.sp, color = color) }
        Text("🔔", fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Preview(name = "Calendar", showBackground = true, heightDp = 800)
@Composable
private fun PreviewCal() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { CalendarScreen() }
}
