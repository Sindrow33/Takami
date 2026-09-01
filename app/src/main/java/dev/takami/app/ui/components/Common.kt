package dev.takami.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

/** Прогресс-точки онбординга (onb-dots): 3 точки 6dp, активная 22dp с градиентом. */
@Composable
fun ProgressDots(total: Int, active: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val isActive = i == active
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (isActive) 22.dp else 6.dp)
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .background(
                        when {
                            isActive -> Brush.horizontalGradient(listOf(Aurora.Acc2, Aurora.AccDim))
                            i < active -> Brush.horizontalGradient(listOf(Aurora.Acc.copy(alpha = .4f), Aurora.Acc.copy(alpha = .4f)))
                            else -> Brush.horizontalGradient(listOf(Aurora.Brd, Aurora.Brd))
                        }
                    )
            )
        }
    }
}

/** Градиентная CTA-кнобка (onb-cta): full width, radius 14, disabled = приглушённая. */
@Composable
fun GradientCta(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Aurora.AccentGradient
                else Brush.linearGradient(listOf(Color(0x0FFFFFFF), Color(0x0FFFFFFF)))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) Color.White else Color.White.copy(alpha = .35f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Карточка-подложка онбординга (onb-card): rgba(255,255,255,.03) + border .08. */
@Composable
fun SubCard(
    modifier: Modifier = Modifier,
    radius: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Aurora.Brd, RoundedCornerShape(radius))
            .padding(14.dp)
    ) { content() }
}

/** Бегающий индикатор загрузки сплэша: 120x3, градиент слева-направо, 1.6s linear infinite. */
@Composable
fun SplashLoader(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "loadSlide")
    val shift by t.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "shift",
    )
    Box(
        modifier
            .width(120.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(Aurora.RadiusFull))
            .background(Color(0x0FFFFFFF))
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.45f)
                .height(3.dp)
                .padding(start = (60 + shift * 60).dp.coerceAtLeast(0.dp))
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Aurora.Acc, Color.Transparent)))
        )
    }
}

/** Пилюля-чип (10.5sp UPPERCASE) — теги на hero-карточках. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(
        modifier
            .clip(RoundedCornerShape(Aurora.RadiusFull))
            .background(Color(0x33000000))
            .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusFull))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text.uppercase(), color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
    }
}

/** Мигающая метка «свечения» — вспомогательный alpha-пульс. */
@Composable
fun Modifier.glowPulse(min: Float = .7f, max: Float = 1f, durationMs: Int = 2500): Modifier {
    val t = rememberInfiniteTransition(label = "glow")
    val a by t.animateFloat(
        initialValue = min, targetValue = max,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = Aurora.Ease), RepeatMode.Reverse),
        label = "alpha",
    )
    return this.alpha(a)
}

internal val IconSize = 22.dp
internal fun Modifier.iconSize() = this.size(IconSize)
