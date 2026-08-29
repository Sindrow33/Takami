package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Сессия чтения: подряд идущие главы с разрывом меньше 6 часов склеены в одну строку. */
data class Session(
    val id: Int,
    val item: FakeTitle,
    val from: Int,
    val to: Int,
    val minutes: Int,
    val day: String,
    val source: String,
)

private fun fakeSessions(): List<Session> {
    var id = 0
    val days = listOf("Сегодня", "Сегодня", "Вчера", "Вчера", "12 мая", "11 мая")
    return Fake.titles.filter { !it.broken }.mapIndexed { i, t ->
        val from = 38 + i * 3
        val span = 1 + (t.id * 7) % 8
        Session(
            id = id++, item = t, from = from, to = from + span,
            minutes = 12 + (t.id * 13) % 70,
            day = days[i % days.size], source = t.source,
        )
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit = {}, onOpen: (FakeTitle) -> Unit = {}) {
    val all = remember { mutableStateListOf<Session>().apply { addAll(fakeSessions()) } }
    var incognito by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = all.filter { query.isBlank() || it.item.name.contains(query, true) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(Dim.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹", fontSize = 24.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(horizontal = Dim.s3),
            )
            Text("История", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "Очистить", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { all.clear() }.padding(end = Dim.s3),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Инкогнито", fontSize = 14.sp)
                Text(
                    if (incognito) "новые главы не записываются"
                    else "чтение попадает в историю",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = incognito, onCheckedChange = { incognito = it })
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(Dim.s8), contentAlignment = Alignment.Center) {
                Text(
                    "История пуста", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                var lastDay = ""
                shown.groupBy { it.day }.forEach { (day, list) ->
                    item(key = "d$day") {
                        Text(
                            day.uppercase(), fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Dim.s4, Dim.s3, Dim.s4, Dim.s2),
                        )
                    }
                    items(list, key = { it.id }) { s -> SessionRow(s) { onOpen(s.item) } }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(s: Session, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Dim.s4, Dim.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dim.s3),
    ) {
        Box(
            Modifier.width(40.dp).height(56.dp)
                .clip(RoundedCornerShape(Dim.rS)).background(s.item.cover)
        )
        Column(Modifier.weight(1f)) {
            Text(s.item.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            val unit = if (s.item.fmt == Fmt.ANIME) "эп." else "гл."
            Text(
                if (s.from == s.to) "$unit ${s.from} · ${s.minutes} мин"
                else "$unit ${s.from}→${s.to} · ${s.minutes} мин",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                s.source, fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "History", showBackground = true, heightDp = 800)
@Composable
private fun PreviewHist() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { HistoryScreen() }
}
