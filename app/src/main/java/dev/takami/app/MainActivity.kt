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
import androidx.compose.ui.unit.dp
import core.engine.ParserStats
import dev.takami.app.data.TakamiPrefs
import dev.takami.app.parser.ParserState
import dev.takami.app.home.HomeScreen
import dev.takami.app.onboarding.OnboardingFlow
import dev.takami.app.ui.components.ModulePlaceholder
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
                Tab.Library -> ModulePlaceholder(
                    "Библиотека", "manga-reader",
                    "Каталог и читалка приезжают из модульной ветки. Здесь останется вход в библиотеку и общий прогресс.",
                )
                Tab.Swipes -> ModulePlaceholder(
                    "Свайпы", "anime-scene-search",
                    "Подбор тайтлов свайпами и поиск по кадру — модуль поиска.",
                )
                Tab.Calendar -> ModulePlaceholder(
                    "Календарь", "app-design",
                    "Полоса дней с цветными точками по типу релиза: аниме, манга, ранобэ.",
                )
                Tab.Settings -> ModulePlaceholder(
                    "Настройки", "autoheal",
                    "Источники, прокси, автопарсер и блок поддержки разработки.",
                )
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
            TabBar(
                active = tab,
                fabLoading = fabLoading,
                onSelect = { tab = it },
                onFab = { fabLoading = true },
            )
        }
    }
}
