package dev.takami.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

/**
 * Заглушка для экранов, которые приезжают из модульных ветек
 * (manga-reader, anime-player, anime-scene-search, autoheal).
 */
@Composable
fun ModulePlaceholder(title: String, branch: String, note: String) {
    Column(
        Modifier.fillMaxSize().background(Aurora.Surface).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(TakamiIcon.Brain, Modifier.size(40.dp), Aurora.Acc2)
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(note, color = Aurora.OnSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Pill("ветка · $branch", tint = Aurora.Acc2)
    }
}
