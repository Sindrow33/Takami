package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun SourcesScreen(onBack: () -> Unit = {}) {
    val a = LocalAurora.current
    val state = remember {
        mutableStateListOf(
            Triple("ReManga", true, "ok"),
            Triple("MangaLib", true, "ok"),
            Triple("MangaDex", true, "slow"),
            Triple("Desu", false, "ok"),
            Triple("AniLibria", true, "ok"),
            Triple("AnimeGo", true, "down"),
            Triple("RanobeLib", true, "ok"),
        )
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
            Text("Источники", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Выключенные источники не участвуют в поиске и автоподборе.",
            fontSize = 12.sp, lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Dim.s4, 0.dp, Dim.s4, Dim.s3),
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            items(state.size) { i ->
                val (name, on, health) = state[i]
                val (dot, label) = when (health) {
                    "down" -> MaterialTheme.colorScheme.error to "недоступен · повтор через 4 мин"
                    "slow" -> a.warn to "медленный ответ"
                    else -> a.ok to "работает"
                }
                Row(
                    Modifier.fillMaxWidth().padding(Dim.s4, Dim.s2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dim.s3),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) dot else MaterialTheme.colorScheme.surfaceVariant))
                    Column(Modifier.weight(1f)) {
                        Text(
                            name, fontSize = 14.sp,
                            color = if (on) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (on) label else "выключен",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = on,
                        onCheckedChange = { state[i] = Triple(name, it, health) },
                    )
                }
            }
        }
    }
}

@Preview(name = "Sources", showBackground = true, heightDp = 800)
@Composable
private fun PreviewSrc() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { SourcesScreen() }
}
