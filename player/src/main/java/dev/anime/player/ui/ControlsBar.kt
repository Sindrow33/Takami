package dev.anime.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anime.player.core.PlaybackState

@Composable
fun ControlsBar(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    val duration = if (state.durationMs > 0L) state.durationMs else 1L
    val progress = if (scrubbing) scrubValue
        else (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val shownMs = if (scrubbing) (scrubValue * duration).toLong() else state.positionMs

    Column(
        modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Slider(
            value = progress,
            onValueChange = { scrubbing = true; scrubValue = it },
            onValueChangeFinished = {
                onSeek((scrubValue * duration).toLong())
                scrubbing = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF7C4DFF),
                activeTrackColor = Color(0xFF7C4DFF),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPlayPause) {
                Text(
                    text = if (state.isPlaying) "II" else "\u25B6",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatTime(shownMs) + " / " + formatTime(state.durationMs),
                color = Color.White,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(16.dp))
            val err = state.error
            if (err != null) {
                Text(text = "ошибка: " + err, color = Color(0xFFFF8A80), fontSize = 12.sp)
            }
        }
    }
}
