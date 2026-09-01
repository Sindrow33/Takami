package dev.anime.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anime.player.track.MediaTrack
import dev.anime.player.track.TrackKind

/**
 * Выбор звуковой дорожки и субтитров — панель поверх видео, как в Tadami.
 *
 * Показывается только если выбирать есть из чего: одна дорожка звука без
 * субтитров означает, что кнопка ведёт в список из одного пункта, и это
 * хуже отсутствия кнопки.
 */
@Composable
fun TrackSheet(
    tracks: List<MediaTrack>,
    onSelect: (MediaTrack) -> Unit,
    onDisableSubtitles: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audio = tracks.filter { it.kind == TrackKind.Audio }
    val subtitles = tracks.filter { it.kind == TrackKind.Subtitle }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.52f)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14161B))
                // Гасим клик по самой панели: иначе тап по пункту сначала
                // закрывал бы её через фон.
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (audio.isNotEmpty()) {
                SectionTitle("Звук")
                audio.forEachIndexed { index, track ->
                    TrackRow(
                        label = track.displayName(index + 1),
                        selected = track.isSelected,
                        onClick = { onSelect(track); onDismiss() },
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            if (subtitles.isNotEmpty()) {
                SectionTitle("Субтитры")
                TrackRow(
                    label = "Выключены",
                    selected = subtitles.none { it.isSelected },
                    onClick = { onDisableSubtitles(); onDismiss() },
                )
                subtitles.forEachIndexed { index, track ->
                    TrackRow(
                        label = track.displayName(index + 1),
                        selected = track.isSelected,
                        onClick = { onSelect(track); onDismiss() },
                    )
                }
            }

            if (audio.isEmpty() && subtitles.isEmpty()) {
                Text(
                    "В этом файле только одна дорожка.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** Есть ли смысл показывать кнопку дорожек. */
fun hasTrackChoice(tracks: List<MediaTrack>): Boolean {
    val audio = tracks.count { it.kind == TrackKind.Audio }
    val subtitles = tracks.count { it.kind == TrackKind.Subtitle }
    return audio > 1 || subtitles > 0
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF7C4DFF).copy(alpha = 0.25f) else Color.Transparent)
            .pointerInput(label) { detectTapGestures { onClick() } }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (selected) "✓" else "",
            color = Color(0xFFA78BFA),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(18.dp),
        )
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}
