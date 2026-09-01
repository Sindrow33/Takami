package dev.takami.app.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.components.Icon
import dev.takami.app.ui.components.TakamiIcon
import dev.takami.app.ui.theme.Aurora
import kotlinx.coroutines.delay

/**
 * Индикатор автопарсера в шапке главной + bottom-sheet со статистикой.
 * Процент — пока демо-сигнал (растёт каждые 8 с), в проде приходит от парсер-движка.
 */
@Composable
fun AiIndicator() {
    var pct by remember { mutableIntStateOf(72) }
    var open by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            if (pct < 99) pct += (1..2).random()
        }
    }

    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(Aurora.RadiusFull))
            .background(Brush.linearGradient(listOf(Aurora.Acc.copy(alpha = .22f), Aurora.AccDim.copy(alpha = .08f))))
            .border(1.dp, Aurora.Acc.copy(alpha = .35f), RoundedCornerShape(Aurora.RadiusFull))
            .clickable { open = true }
            .padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(Aurora.RadiusFull)).background(Aurora.AccentGradient),
            contentAlignment = Alignment.Center,
        ) { Icon(TakamiIcon.Brain, Modifier.size(13.dp), Color.White) }
        MiniChart()
        Text("$pct%", color = Color(0xFFE4DAFF), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }

    if (open) AiSheet(pct) { open = false }
}

/** Мини-график: 5 столбиков, scaleY 0.6→1, stagger 150 мс. */
@Composable
private fun MiniChart() {
    val t = rememberInfiniteTransition(label = "aiBar")
    Row(
        Modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(5) { i ->
            val h by t.animateFloat(
                initialValue = .6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(2000, easing = Aurora.Ease),
                    RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 150),
                ),
                label = "bar$i",
            )
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Aurora.Acc2)
            )
        }
    }
}

private data class AiStat(val label: String, val value: String, val tone: Color)

@Composable
private fun AiSheet(pct: Int, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onClose)
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1D25), Color(0xFF12141A))))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(Aurora.RadiusFull)).background(Aurora.AccentGradient),
                    contentAlignment = Alignment.Center,
                ) { Icon(TakamiIcon.Brain, Modifier.size(22.dp), Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("Автопарсер · обучаемость", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Самовосстанавливающийся движок. Учится на каждом запросе.",
                        color = Aurora.OnSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp,
                    )
                }
                Text("✕", color = Aurora.OnSurfaceVariant, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onClose))
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(pct)
                Text(
                    "Модель дообучается на ваших запросах. Данные не покидают устройство — веса и кеш лежат в локальном хранилище.",
                    color = Aurora.OnSurfaceVariant, fontSize = 12.sp, lineHeight = 19.sp,
                )
            }

            Spacer(Modifier.height(20.dp))
            val stats = listOf(
                AiStat("Источников", "14", Color.White),
                AiStat("Самопочинок", "38", Aurora.Ok),
                AiStat("Точность", "96%", Aurora.Ok),
                AiStat("Аномалий", "2", Aurora.Warn),
            )
            stats.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { s ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x08FFFFFF))
                                .padding(14.dp)
                        ) {
                            Text(s.value, color = s.tone, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(s.label, color = Aurora.OnSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(6.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 96.dp)
                    .clip(RoundedCornerShape(Aurora.RadiusM))
                    .background(Color(0x47000000))
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                val log = listOf(
                    Triple("[3 мин]", "ReadManga · сменилась структура кнопок глав", Aurora.Ok),
                    Triple("[18 мин]", "AnimeGo · селектор плеера восстановлен", Aurora.Ok),
                    Triple("[1 ч]", "RanobeLib · таймаут, повтор через прокси", Aurora.Warn),
                    Triple("[3 ч]", "MangaLib · новая разметка списка глав", Aurora.Ok),
                )
                log.forEach { (ts, msg, tone) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(ts, color = Aurora.Acc2, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
                        Text(msg, color = tone, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Кольцо прогресса 92dp, stroke 8, градиент Acc2 → AccDim, round cap. */
@Composable
private fun ProgressRing(pct: Int) {
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 8.dp.toPx()
            val inset = sw / 2f
            drawArc(
                color = Color(0x14FFFFFF), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                style = Stroke(sw),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(Aurora.Acc2, Aurora.AccDim, Aurora.Acc2)),
                startAngle = -90f, sweepAngle = 360f * pct / 100f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Text("$pct%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
