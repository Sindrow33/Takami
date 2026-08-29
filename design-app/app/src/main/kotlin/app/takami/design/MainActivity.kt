package app.takami.design

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by remember { mutableStateOf(true) }
            var tab by remember { mutableIntStateOf(0) }
            TakamiTheme(dark = dark) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            listOf("⌂ Главная", "▤ Библиотека", "▦ Календарь", "⋯ Ещё")
                                .forEachIndexed { i, t ->
                                    NavigationBarItem(
                                        selected = tab == i,
                                        onClick = { tab = i },
                                        icon = { Text(t.take(1), fontSize = 18.sp) },
                                        label = { Text(t.drop(2), fontSize = 10.sp) },
                                    )
                                }
                        }
                    },
                ) { pad ->
                    Box(Modifier.padding(pad).background(MaterialTheme.colorScheme.background)) {
                        when (tab) {
                            0 -> HomeScreen()
                            else -> Column(Modifier.fillMaxSize().padding(24.dp)) {
                                Text("Экран в работе", fontSize = 16.sp)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { dark = !dark }) {
                                    Text(if (dark) "Светлая тема" else "Тёмная тема")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
