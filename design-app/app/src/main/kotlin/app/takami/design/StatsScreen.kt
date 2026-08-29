package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatsScreen(onBack: () -> Unit = {}) {
    val a = LocalAurora.current
    val week = listOf(4, 7, 2, 9, 5, 12, 6)
    val days = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    val maxV = week.max()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(Dim.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹", fontSize = 24.sp,
                    modifier = Modifier.clickable(onClick = onBack).padding(horizontal = Dim.s3),
                )
                Text("Прогресс", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            AuroraSurface(
                Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
                level = SurfaceLevel.Strong,
            ) {
                Column(Modifier.padding(Dim.s4)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Уровень 7", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "1 240 / 1 800 опыта",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🔥 12", fontSize = 18.sp, color = a.warn)
                            Text(
                                "дней подряд", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(Dim.s3))
                    ProgressLine(69, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
                horizontalArrangement = Arrangement.spacedBy(Dim.s2),
            ) {
                listOf("342" to "глав", "58 ч" to "времени", "19" to "тайтлов").forEach { (v, k) ->
                    AuroraSurface(Modifier.weight(1f), SurfaceLevel.Subtle, Dim.rM) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = Dim.s3),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(v, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                k, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item { SectionHeader("За неделю") }
        item {
            Row(
                Modifier.fillMaxWidth().height(120.dp).padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
                horizontalArrangement = Arrangement.spacedBy(Dim.s2),
                verticalAlignment = Alignment.Bottom,
            ) {
                week.forEachIndexed { i, v ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("$v", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((v.toFloat() / maxV * 74).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (v == maxV) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                        Text(
                            days[i], fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        item { SectionHeader("Достижения") }
        item {
            Column(Modifier.padding(horizontal = Dim.s4)) {
                listOf(
                    Triple("📚", "Марафонец", "10 глав за день — получено"),
                    Triple("🌙", "Полуночник", "чтение после 2:00 — получено"),
                    Triple("🎯", "Сотня", "100 глав в одном тайтле — 42 из 100"),
                ).forEachIndexed { i, (ico, name, hint) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = Dim.s2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dim.s3),
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(Dim.rM))
                                .background(
                                    if (i < 2) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center,
                        ) { Text(ico, fontSize = 18.sp) }
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 14.sp)
                            Text(
                                hint, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Stats", showBackground = true, heightDp = 900)
@Composable
private fun PreviewStats() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { StatsScreen() }
}
