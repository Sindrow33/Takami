package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MoreRow(val route: String, val icon: String, val label: String, val hint: String)

private val rows = listOf(
    MoreRow("updates", "↻", "Обновления", "новые главы и серии"),
    MoreRow("downloads", "⤓", "Загрузки", "очередь и офлайн"),
    MoreRow("history", "◷", "История", "что и когда читали"),
    MoreRow("stats", "◆", "Прогресс", "серии и достижения"),
    MoreRow("sources", "⛭", "Источники", "состояние парсеров"),
    MoreRow("account", "◉", "Аккаунт", "вход и синхронизация"),
)

@Composable
fun MoreScreen(dark: Boolean, onTheme: (Boolean) -> Unit, onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Text(
                "Ещё", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(Dim.s4, Dim.s3),
            )
        }
        item {
            AuroraSurface(
                Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
                level = SurfaceLevel.Strong,
            ) {
                Row(
                    Modifier.padding(Dim.s4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dim.s3),
                ) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) { Text("?", fontSize = 18.sp) }
                    Column(Modifier.weight(1f)) {
                        Text("Гость", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "прогресс хранится только на устройстве",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Войти", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpen("account") },
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Тёмная тема", fontSize = 14.sp)
                    Text(
                        "палитра Aurora, фиолетовый акцент",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = dark, onCheckedChange = onTheme)
            }
        }
        items(rows, key = { it.route }) { r ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(r.route) }.padding(Dim.s4, Dim.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dim.s3),
            ) {
                Text(r.icon, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(r.label, fontSize = 14.sp)
                    Text(
                        r.hint, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StubScreen(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(Dim.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹", fontSize = 24.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(horizontal = Dim.s3),
            )
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxSize().padding(Dim.s6), contentAlignment = Alignment.Center) {
            Text(
                "Экран в очереди на вёрстку",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "More", showBackground = true, heightDp = 800)
@Composable
private fun PreviewMore() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) {
        MoreScreen(dark = true, onTheme = {}, onOpen = {})
    }
}
