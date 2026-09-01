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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import core.engine.ParserStats
import dev.takami.app.ui.components.Icon
import dev.takami.app.ui.components.TakamiIcon
import dev.takami.app.ui.theme.Aurora

/**
 * Индикатор автопарсера в шапке главной + bottom-sheet со статистикой.
 * Данные — реальные, из модуля `:autoheal`: процент обучаемости, число
 * источников, самопочинок, точность и аномалии считаются по накопленной
 * истории разборов. Пока ни один источник не подключён, показывается
 * прочерк вместо выдуманного числа.
 */
@Composable
fun AiIndicator(stats: ParserStats) {
    var open by remember { mutableStateOf(false) }
    val pct = stats.learningPercent
    val hasData = stats.sourceCount > 0

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
        Text(
            if (hasData) "$pct%" else "—",
            color = Color(0xFFE4DAFF), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
        )
    }

    if (open) {
        /*
         * Лист обязан жить в Dialog, а не в дереве главной.
         * Главная — вертикально скроллящаяся колонка, то есть высота
         * её содержимого не ограничена. Composable с fillMaxSize внутри
         * такого родителя роняет приложение на измерении:
         * "Vertically scrollable component was measured with an infinity
         * maximum height constraints". Ровно этот краш и ловился при
         * нажатии на индикатор автопарсера.
         */
        Dialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AiSheet(stats) { open = false }
        }
    }
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
private fun AiSheet(stats: ParserStats, onClose: () -> Unit) {
    val hasData = stats.sourceCount > 0
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClose,
            )
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Потолок высоты обязателен: содержимое листа растёт вместе
                // с логом, а скроллящийся контейнер без границы сверху —
                // тот же самый краш на измерении.
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1D25), Color(0xFF12141A))))
                .verticalScroll(rememberScrollState())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .padding(20.dp)
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
                ProgressRing(stats.learningPercent, hasData)
                Text(
                    if (hasData)
                        "Движок дообучается на каждом разборе. Данные не покидают устройство — история и конфиги лежат в локальном хранилище."
                    else
                        "Источники ещё не подключены. Движок начнёт учиться после первых разборов.",
                    color = Aurora.OnSurfaceVariant, fontSize = 12.sp, lineHeight = 19.sp,
                )
            }

            Spacer(Modifier.height(20.dp))
            val cells = listOf(
                AiStat("Источников", "${stats.sourceCount}", Color.White),
                AiStat("Самопочинок", "${stats.selfHealCount}", Aurora.Ok),
                AiStat("Точность", if (hasData) "${stats.accuracyPercent}%" else "—", Aurora.Ok),
                AiStat(
                    "Аномалий", "${stats.anomalyCount}",
                    if (stats.anomalyCount > 0) Aurora.Warn else Aurora.Ok,
                ),
            )
            cells.chunked(2).forEach { row ->
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

            /*
             * Разбор сайта живёт здесь, а не отдельной кнопкой на
             * главной: это действие над движком, сводку которого
             * пользователь сейчас и смотрит. Отдельная карточка в
             * быстрых действиях уводила на пустой экран, который никто
             * не связывал с автопарсером.
             */
            Spacer(Modifier.height(4.dp))
            dev.takami.app.parser.AutoParseInline()

            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 96.dp)
                    .clip(RoundedCornerShape(Aurora.RadiusM))
                    .background(Color(0x47000000))
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                if (stats.log.isEmpty()) {
                    Text(
                        "лог пуст — разборов ещё не было",
                        color = Aurora.OnSurfaceVariant, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace,
                    )
                }
                stats.log.forEach { entry ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "[${ago(entry.atMillis)}]",
                            color = Aurora.Acc2, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            "${entry.host} · ${entry.message}",
                            color = when (entry.tone) {
                                ParserStats.Tone.OK -> Aurora.Ok
                                ParserStats.Tone.WARN -> Aurora.Warn
                                ParserStats.Tone.ERROR -> Aurora.Error
                            },
                            fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Кольцо прогресса 92dp, stroke 8, градиент Acc2 → AccDim, round cap. */
@Composable
private fun ProgressRing(pct: Int, hasData: Boolean = true) {
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
        Text(
            if (hasData) "$pct%" else "—",
            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
        )
    }
}

/** «3 мин» / «2 ч» / «4 дн» — относительное время для лога. */
private fun ago(atMillis: Long): String {
    if (atMillis <= 0L) return "только что"
    val delta = (System.currentTimeMillis() - atMillis).coerceAtLeast(0L) / 1000
    return when {
        delta < 60 -> "только что"
        delta < 3600 -> "${delta / 60} мин"
        delta < 86_400 -> "${delta / 3600} ч"
        else -> "${delta / 86_400} дн"
    }
}
