package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TitleScreen(item: FakeTitle, onBack: () -> Unit = {}) {
    var broken by remember { mutableStateOf(setOf<String>()) }
    var pinnedId by remember { mutableStateOf<String?>(null) }
    val best = FakeSources.pool.first()
    val current = pinnedId
        ?.let { id -> FakeSources.pool.firstOrNull { it.id == id && it.id !in broken } }
        ?: FakeSources.pick(broken)
    val progress = 42

    LazyColumn(Modifier.fillMaxSize()) {
        item { TopRow(onBack) }
        item { Hero(item) }
        item { FormatRow(item.fmt) }
        item { ActionRow() }
        item {
            SourceBar(current = current, best = best, pinned = pinnedId != null) {
                // демо: ломаем самый полный живой источник
                val victim = FakeSources.pick(broken)
                broken = if (victim != null) broken + victim.id else emptySet()
            }
        }
        if (current != null && progress > current.maxChapter) {
            item { Warn("Здесь только до гл. ${current.maxChapter}, а вы на $progress") }
        }
        item { Description() }
        item { SectionHeader("Персонажи", "Все ›") }
        item { CharRail() }
        item { SectionHeader("Обсуждение", "Все 4 ›") }
        item { CommentPreview() }
        item { SectionHeader("Главы") }
        if (current == null) {
            item { Warn("Список глав недоступен — все источники отвалились") }
        } else {
            items(FakeSources.chapters(current, progress), key = { it.number }) { ch ->
                ChapterRow(ch)
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun TopRow(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(Dim.s3, Dim.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("‹", fontSize = 24.sp, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = Dim.s3))
        Spacer(Modifier.weight(1f))
        Text("⋮", fontSize = 18.sp, modifier = Modifier.padding(horizontal = Dim.s3))
    }
}

@Composable
private fun Hero(item: FakeTitle) {
    Row(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
        horizontalArrangement = Arrangement.spacedBy(Dim.s4),
    ) {
        Box(
            Modifier.width(108.dp).height(162.dp)
                .clip(RoundedCornerShape(Dim.rM)).background(item.cover)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp)
            Text("2023 · ★ 8.7 · онгоинг", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Dim.s1)) {
                listOf("Экшен", "Фэнтези").forEach { g ->
                    Box(
                        Modifier.clip(RoundedCornerShape(Dim.rS))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(g, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatRow(active: Fmt) {
    Row(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
        horizontalArrangement = Arrangement.spacedBy(Dim.s2),
    ) {
        Fmt.entries.forEach { f ->
            val on = f == active
            AuroraSurface(
                Modifier.weight(1f),
                level = if (on) SurfaceLevel.Strong else SurfaceLevel.Subtle,
                radius = Dim.rM,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = Dim.s3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        f.title, fontSize = 13.sp,
                        color = if (on) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (on) "124 гл." else "нет",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow() {
    Row(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
        horizontalArrangement = Arrangement.spacedBy(Dim.s2),
    ) {
        Button(
            onClick = {}, modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) { Text("В библиотеке", fontSize = 13.sp) }
        Button(
            onClick = {}, modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(999.dp),
        ) { Text("Продолжить", fontSize = 13.sp) }
    }
}

@Composable
private fun Warn(text: String) {
    val a = LocalAurora.current
    Box(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s3)
            .clip(RoundedCornerShape(Dim.rM))
            .background(a.warn.copy(alpha = 0.12f))
            .padding(Dim.s3)
    ) { Text(text, fontSize = 12.sp, color = a.warn, lineHeight = 16.sp) }
}

@Composable
private fun Description() {
    Text(
        "Обычный старшеклассник получает силу, о которой не просил, и теперь " +
            "должен разобраться, кому она понадобилась и почему за ней охотятся.",
        fontSize = 14.sp, lineHeight = 21.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
    )
}

@Composable
private fun CharRail() {
    Row(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
        horizontalArrangement = Arrangement.spacedBy(Dim.s3),
    ) {
        listOf("Главный герой", "Наставник", "Соперник").forEach { n ->
            Column(Modifier.width(82.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Text(
                    n, fontSize = 11.sp, lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CommentPreview() {
    AuroraSurface(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
        level = SurfaceLevel.Subtle,
        radius = Dim.rM,
    ) {
        Column(Modifier.padding(Dim.s3)) {
            Text("Kirito_99 · 5 ч", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Глава 43 просто разнесла. Не ожидал такого поворота с наставником.",
                fontSize = 14.sp, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ChapterRow(ch: FakeChapter) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = ch.available) {}
            .padding(Dim.s4, Dim.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                ch.name, fontSize = 14.sp,
                color = when {
                    !ch.available -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    ch.read -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                if (ch.available) ch.date else "нет в этом источнике",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        if (ch.available) Text("⤓", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Title", showBackground = true, heightDp = 1000)
@Composable
private fun PreviewTitle() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) {
        TitleScreen(Fake.titles.first())
    }
}
