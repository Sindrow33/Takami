package dev.anime.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anime.player.core.PlaybackState

/**
 * Нижняя панель управления. Раскладка как в Tadami: слайдер с буфером,
 * время, кнопки шага на 10 секунд по бокам от play/pause.
 */
@Composable
fun ControlsBar(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onStepBack: (() -> Unit)? = null,
    onStepForward: (() -> Unit)? = null,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    val duration = if (state.durationMs > 0L) state.durationMs else 1L
    val progress = if (scrubbing) scrubValue
        else (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val buffered = (state.bufferedMs.toFloat() / duration).coerceIn(0f, 1f)
    val shownMs = if (scrubbing) (scrubValue * duration).toLong() else state.positionMs

    Column(
        modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            // Полоса буфера под слайдером: без неё на медленной сети непонятно,
            // подгрузилось ли уже дальше или плеер встал.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .size(height = 4.dp, width = 0.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .fillMaxWidth(buffered)
                    .size(height = 4.dp, width = 0.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
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
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onStepBack != null) {
                IconPill("−10", onStepBack)
                Spacer(Modifier.width(8.dp))
            }
            IconPill(if (state.isPlaying) "II" else "\u25B6", onPlayPause, wide = true)
            if (onStepForward != null) {
                Spacer(Modifier.width(8.dp))
                IconPill("+10", onStepForward)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = formatTime(shownMs) + " / " + formatTime(state.durationMs),
                color = Color.White,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(12.dp))
            val err = state.error
            if (err != null) {
                Text(text = "ошибка: " + err, color = Color(0xFFFF8A80), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun IconPill(label: String, onClick: () -> Unit, wide: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .size(width = if (wide) 54.dp else 46.dp, height = 36.dp)
            .pointerInput(label) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = if (wide) 17.sp else 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
