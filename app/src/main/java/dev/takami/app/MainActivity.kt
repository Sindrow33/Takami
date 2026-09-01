package dev.takami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import core.engine.ParserStats
import dev.takami.app.data.TakamiPrefs
import dev.takami.app.parser.ParserState
import dev.takami.app.calendar.CalendarScreen
import dev.takami.app.home.HomeScreen
import dev.takami.app.library.LibraryTabs
import dev.takami.app.onboarding.OnboardingFlow
import dev.takami.swipes.SwipesScreen
import dev.takami.app.settings.SettingsScreen
import dev.takami.app.ui.components.Tab
import dev.takami.app.ui.components.TabBar
import dev.takami.app.ui.theme.Aurora
import dev.takami.app.ui.theme.TakamiTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = TakamiPrefs(this)
        val parser = ParserState(this)
        setContent {
            TakamiTheme {
                TakamiApp(prefs, parser)
            }
        }
    }
}

@Composable
fun TakamiApp(prefs: TakamiPrefs, parser: ParserState) {
    var onboarded by remember { mutableStateOf(prefs.onboarded) }

    Box(Modifier.fillMaxSize().background(Aurora.Surface)) {
        if (!onboarded) {
            OnboardingFlow {
                prefs.onboarded = true
                onboarded = true
            }
        } else {
            MainShell(parser)
        }
    }
}

@Composable
private fun MainShell(parser: ParserState) {
    var tab by remember { mutableStateOf(Tab.Home) }
    var fabLoading by remember { mutableStateOf(false) }
    /*
     * Полноэкранный режим: во время видео нижней панели нет, а экран
     * разворачивается горизонтально. Ориентацией и системными полосами
     * управляет активити, поэтому решение принимается здесь, а плеер
     * только сообщает, что идёт воспроизведение.
     */
    var immersive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(immersive) {
        val activity = context as? android.app.Activity ?: return@LaunchedEffect
        activity.requestedOrientation = if (immersive) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (immersive) hide(WindowInsetsCompat.Type.systemBars())
            else show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Сводка автопарсера читается с диска — обновляем при возврате на главную.
    var parserStats by remember { mutableStateOf(ParserStats.EMPTY) }
    LaunchedEffect(tab) {
        if (tab == Tab.Home) parserStats = parser.stats()
    }

    LaunchedEffect(fabLoading) {
        if (fabLoading) {
            delay(1200)          // длительность псевдо-загрузки из хендоффа
            fabLoading = false
            tab = Tab.Swipes
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Каскадный вход экрана: slide -18dp + fade, как cascadeIn в прототипе
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (slideInVertically(tween(500, easing = Aurora.EaseOut)) { -18 } +
                    fadeIn(tween(500))) togetherWith fadeOut(tween(160))
            },
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            label = "screen",
        ) { current ->
            when (current) {
                Tab.Home -> HomeScreen(parserStats)
                Tab.Library -> LibraryTabs(onImmersive = { immersive = it })
                Tab.Swipes -> SwipesScreen()
                Tab.Calendar -> CalendarScreen()
                Tab.Settings -> SettingsScreen()
            }
        }

        // Отступ под системную навигацию теперь внутри TabBar: он
        // обязан быть под фоном панели, а не поднимать её над полосой.
        if (!immersive) Box(Modifier.align(Alignment.BottomCenter)) {
            TabBar(
                active = tab,
                fabLoading = fabLoading,
                onSelect = { tab = it },
                onFab = { fabLoading = true },
            )
        }
    }
}
