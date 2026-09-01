package dev.anime.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.anime.player.core.Media3Engine
import dev.anime.player.skip.SkipSegment
import dev.anime.player.skip.activeSegmentAt
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    engine: Media3Engine,
    segments: List<SkipSegment>,
    autoSkip: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val state by engine.state.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    var autoSkipped by remember { mutableStateOf<SkipSegment?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    val active = remember(segments, state.positionMs) {
        activeSegmentAt(segments, state.positionMs)
    }

    LaunchedEffect(active, autoSkip) {
        val seg = active
        if (autoSkip && seg != null && seg != autoSkipped) {
            autoSkipped = seg
            engine.seekTo(seg.endMs)
            toast = seg.label.replace("Пропустить", "Пропущен")
            delay(4000)
            toast = null
        }
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    player = engine.exoInstance()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { offset ->
                            val forward = offset.x > size.width / 2
                            val delta = if (forward) 10000L else -10000L
                            engine.seekTo(engine.state.value.positionMs + delta)
                        },
                    )
                }
        )

        if (state.isBuffering && state.error == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }

        val seg = active
        if (seg != null && !autoSkip) {
            Button(
                onClick = { engine.seekTo(seg.endMs) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 88.dp),
            ) { Text(seg.label) }
        }

        val msg = toast
        if (msg != null) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(msg, color = Color.White)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "ВЕРНУТЬ",
                    color = Color(0xFF9C7CFF),
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            val s = autoSkipped
                            if (s != null) engine.seekTo(s.startMs)
                            toast = null
                        }
                    },
                )
            }
        }

        if (controlsVisible) {
            ControlsBar(
                state = state,
                onPlayPause = { if (state.isPlaying) engine.pause() else engine.play() },
                onSeek = { engine.seekTo(it) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
