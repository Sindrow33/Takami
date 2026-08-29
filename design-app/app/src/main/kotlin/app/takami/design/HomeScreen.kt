package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..4 -> "Доброй ночи"
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    else -> "Добрый вечер"
}

@Composable
fun HomeScreen(onOpen: (FakeTitle) -> Unit = {}) {
    val a = LocalAurora.current
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().background(a.heroGradient).padding(Dim.s4, Dim.s6)) {
                Text(greeting(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Читатель", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dim.s4))
            }
        }
        item { HeroCard(Fake.continueItem, onOpen) }
        item { QuickRow() }
        item { SectionHeader("Продолжить чтение", "Все ›") }
        item { Rail(Fake.reading, onOpen) }
        Fmt.entries.forEach { f ->
            val list = Fake.byFmt(f)
            if (list.isNotEmpty()) {
                item { SectionHeader(f.title, "Все ›") }
                item { Rail(list, onOpen) }
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun HeroCard(item: FakeTitle, onOpen: (FakeTitle) -> Unit) {
    AuroraSurface(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
        level = SurfaceLevel.Strong,
    ) {
        Row(Modifier.padding(Dim.s4), horizontalArrangement = Arrangement.spacedBy(Dim.s4)) {
            Box(
                Modifier.width(78.dp).height(117.dp)
                    .clip(RoundedCornerShape(Dim.rM)).background(item.cover)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Продолжить", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                Text(
                    "${item.sub} · ${item.source}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProgressLine(item.progress, Modifier.fillMaxWidth())
                Button(
                    onClick = { onOpen(item) },
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(18.dp, 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                ) { Text("Продолжить", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun QuickRow() {
    val tiles = listOf(
        Triple("↻", "Обновления", "8"),
        Triple("▦", "Календарь", "сегодня"),
        Triple("⤓", "Загрузки", "3"),
        Triple("◆", "Прогресс", "серия"),
    )
    Row(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
        horizontalArrangement = Arrangement.spacedBy(Dim.s2),
    ) {
        tiles.forEach { (ico, label, hint) ->
            AuroraSurface(Modifier.weight(1f), level = SurfaceLevel.Subtle, radius = Dim.rM) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = Dim.s3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(ico, fontSize = 19.sp, color = MaterialTheme.colorScheme.primary)
                    Text(label, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(hint, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun Rail(list: List<FakeTitle>, onOpen: (FakeTitle) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dim.s4),
        horizontalArrangement = Arrangement.spacedBy(Dim.s3),
        modifier = Modifier.padding(bottom = Dim.s4),
    ) {
        items(list, key = { it.id }) { TitleCard(it) { onOpen(it) } }
    }
}

@Preview(name = "Home dark", showBackground = true, heightDp = 900)
@Composable
private fun PreviewDark() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { HomeScreen() }
}

@Preview(name = "Home light", showBackground = true, heightDp = 900)
@Composable
private fun PreviewLight() = TakamiTheme(dark = false) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { HomeScreen() }
}
