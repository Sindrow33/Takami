package app.takami.design

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by rememberSaveable { mutableStateOf(true) }
            var tab by rememberSaveable { mutableIntStateOf(0) }
            var openId by rememberSaveable { mutableStateOf<Int?>(null) }
            var route by rememberSaveable { mutableStateOf<String?>(null) }
            val opened = openId?.let { id -> Fake.titles.firstOrNull { it.id == id } }

            BackHandler(enabled = opened != null || route != null) {
                if (opened != null) openId = null else route = null
            }

            TakamiTheme(dark = dark) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (opened == null && route == null) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                val tabs = listOf(
                                    "⌂" to "Главная", "▤" to "Библиотека",
                                    "▦" to "Календарь", "⋯" to "Ещё",
                                )
                                tabs.forEachIndexed { i, (ico, label) ->
                                    NavigationBarItem(
                                        selected = tab == i,
                                        onClick = { tab = i },
                                        icon = { Text(ico, fontSize = 18.sp) },
                                        label = { Text(label, fontSize = 10.sp) },
                                    )
                                }
                            }
                        }
                    },
                ) { pad ->
                    Box(
                        Modifier
                            .padding(pad)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when {
                            opened != null -> TitleScreen(opened) { openId = null }
                            route == "downloads" -> DownloadsScreen { route = null }
                            route == "history" -> HistoryScreen({ route = null }) {
                                route = null; openId = it.id
                            }
                            route == "stats" -> StatsScreen { route = null }
                            route == "sources" -> SourcesScreen { route = null }
                            route == "account" -> AccountScreen { route = null }
                            route != null -> StubScreen(routeTitle(route!!)) { route = null }
                            tab == 0 -> HomeScreen { openId = it.id }
                            tab == 1 -> LibraryScreen { openId = it.id }
                            tab == 2 -> CalendarScreen { openId = it.id }
                            else -> MoreScreen(dark, { dark = it }) { route = it }
                        }
                    }
                }
            }
        }
    }
}

private fun routeTitle(r: String) = when (r) {
    "updates" -> "Обновления"
    "history" -> "История"
    "stats" -> "Прогресс"
    "sources" -> "Источники"
    "account" -> "Аккаунт"
    else -> "Экран"
}

@Composable
private fun Placeholder(dark: Boolean, toggle: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(Dim.s6)) {
        Text("Экран в работе", fontSize = 16.sp)
        Spacer(Modifier.height(Dim.s3))
        Button(onClick = toggle) {
            Text(if (dark) "Светлая тема" else "Тёмная тема")
        }
        Spacer(Modifier.height(Dim.s4))
        Text(
            "Готово: главная, библиотека, карточка тайтла с подбором источника. " +
                "Дальше — читалка, календарь, загрузки.",
            fontSize = 13.sp, lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
