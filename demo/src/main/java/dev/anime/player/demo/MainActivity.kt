package dev.anime.player.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.anime.player.core.Media3Engine
import dev.anime.player.skip.FakeSkipProvider
import dev.anime.player.skip.SkipSegment
import dev.anime.player.ui.PlayerScreen

// Публичные тестовые потоки, парсеры не нужны.
private const val MP4 =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
private const val HLS =
    "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

class MainActivity : ComponentActivity() {

    private lateinit var engine: Media3Engine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        engine = Media3Engine(this)
        engine.load(MP4)

        setContent {
            val state by engine.state.collectAsState()
            var segments by remember { mutableStateOf<List<SkipSegment>>(emptyList()) }

            LaunchedEffect(state.durationMs) {
                if (state.durationMs > 0L && segments.isEmpty()) {
                    segments = FakeSkipProvider().segments(null, 1, state.durationMs)
                }
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                PlayerScreen(
                    engine = engine,
                    segments = segments,
                    autoSkip = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        engine.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
