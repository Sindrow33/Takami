package dev.takami.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

enum class Tab { Home, Library, Swipes, Calendar, Settings }

/**
 * Нижний таббар: 5 позиций, центральная — FAB 56dp со смещением -26dp.
 * При загрузке FAB пульсирует и показывает спиннер (1200 мс), потом открывает «Свайпы».
 */
@Composable
fun TabBar(
    active: Tab,
    fabLoading: Boolean,
    calendarBadge: Int = 3,
    onSelect: (Tab) -> Unit,
    onFab: () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Aurora.SurfaceContainer)
                .border(1.dp, Aurora.Brd)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(TakamiIcon.Home, "Главная", active == Tab.Home, Modifier.weight(1f)) { onSelect(Tab.Home) }
            TabItem(TakamiIcon.Library, "Библиотека", active == Tab.Library, Modifier.weight(1f)) { onSelect(Tab.Library) }
            Spacer(Modifier.weight(1f))
            TabItem(TakamiIcon.Calendar, "Календарь", active == Tab.Calendar, Modifier.weight(1f), badge = calendarBadge) { onSelect(Tab.Calendar) }
            TabItem(TakamiIcon.Settings, "Настройки", active == Tab.Settings, Modifier.weight(1f)) { onSelect(Tab.Settings) }
        }
        Fab(fabLoading, Modifier.align(Alignment.TopCenter).offset(y = (-26).dp), onFab)
    }
}

@Composable
private fun TabItem(
    icon: TakamiIcon,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    onClick: () -> Unit,
) {
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(icon, Modifier.size(22.dp), if (selected) Aurora.Primary else Aurora.OnSurfaceVariant)
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(14.dp)
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(Aurora.Acc),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (selected) Aurora.Primary else Aurora.OnSurfaceVariant, fontSize = 9.5.sp)
    }
}

@Composable
private fun Fab(loading: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "fab")
    val pulse by t.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(550, easing = Aurora.Ease), RepeatMode.Reverse),
        label = "pulse",
    )
    val spin by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "spin",
    )

    Box(
        modifier
            .size(56.dp)
            .scale(if (loading) pulse else 1f)
            .clip(RoundedCornerShape(Aurora.RadiusFull))
            .background(Aurora.AccentGradient)
            .border(6.dp, Aurora.SurfaceContainer, RoundedCornerShape(Aurora.RadiusFull))
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            Canvas(Modifier.size(26.dp).rotate(spin)) {
                drawArc(
                    color = Color.White, startAngle = 0f, sweepAngle = 228f, useCenter = false,
                    style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        } else {
            Icon(TakamiIcon.Swipes, Modifier.size(24.dp), Color.White)
        }
    }
}
