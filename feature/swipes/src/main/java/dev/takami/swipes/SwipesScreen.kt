package dev.takami.swipes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora
import kotlinx.coroutines.launch

/**
 * Экран вкладки «Свайпы»: подбор тайтлов карточками.
 *
 * Точка входа модуля — без обязательных параметров, хост подключает как
 * `SwipesScreen()`. Вся арифметика жеста (порог, наклон, метки, вылет)
 * живёт в [SwipeMath] и покрыта JVM-тестами; здесь только отрисовка и анимация.
 *
 * Колода пока демонстрационная: подбор поверх реального каталога включается,
 * когда каталог появится — форма [DeckCard] под него и подобрана.
 */
@Composable
fun SwipesScreen(modifier: Modifier = Modifier) {
    var deck by remember { mutableStateOf(DeckState(SwipeMath.demoDeck())) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val dragX = remember { Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun commit(direction: SwipeDirection) {
        if (direction == SwipeDirection.None) {
            scope.launch { dragX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 420f)) }
            return
        }
        scope.launch {
            dragX.animateTo(SwipeMath.flyAwayX(direction, widthPx), tween(220))
            deck = deck.apply(direction)
            dragX.snapTo(0f)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .padding(horizontal = 20.dp)
            .onSizeChanged { widthPx = it.width.toFloat() },
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Свайпы", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            if (!deck.isFinished) {
                Text(
                    "осталось " + deck.remaining,
                    color = Aurora.OnSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Вправо — хочу смотреть, влево — мимо.",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(18.dp))

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (deck.isFinished) {
                DeckFinished(liked = deck.liked.size, onRestart = { deck = deck.restart() })
            } else {
                deck.next?.let { under ->
                    DeckCardView(
                        card = under,
                        modifier = Modifier
                            .scale(SwipeMath.underCardScale(dragX.value, widthPx))
                            .alpha(0.55f),
                    )
                }
                deck.current?.let { top ->
                    val progress = SwipeMath.dragProgress(dragX.value, widthPx)
                    Box(
                        Modifier
                            .offset { IntOffset(dragX.value.toInt(), 0) }
                            .rotate(SwipeMath.tiltDegrees(dragX.value, widthPx))
                            .pointerInput(top.id, widthPx) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        commit(SwipeMath.directionFor(dragX.value, widthPx))
                                    },
                                    onDragCancel = { commit(SwipeDirection.None) },
                                    onHorizontalDrag = { _, delta ->
                                        scope.launch { dragX.snapTo(dragX.value + delta) }
                                    },
                                )
                            },
                    ) {
                        DeckCardView(card = top, glow = progress)
                        SwipeBadges(
                            alpha = SwipeMath.badgeAlpha(dragX.value, widthPx),
                            positive = progress > 0f,
                        )
                    }
                }
            }
        }

        if (!deck.isFinished) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChoiceButton("Мимо", Aurora.Error) { commit(SwipeDirection.Skip) }
                Spacer(Modifier.width(14.dp))
                ChoiceButton("Хочу", Aurora.Ok) { commit(SwipeDirection.Like) }
            }
        }
        Spacer(Modifier.height(96.dp)) // место под нижнюю панель
    }
}

@Composable
private fun DeckCardView(card: DeckCard, modifier: Modifier = Modifier, glow: Float = 0f) {
    val tint = when {
        glow > 0f -> Aurora.Ok.copy(alpha = 0.35f * glow)
        glow < 0f -> Aurora.Error.copy(alpha = 0.35f * -glow)
        else -> Aurora.Outline.copy(alpha = 0.5f)
    }
    Column(
        modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Aurora.SurfaceContainer, Aurora.ScLow)
                )
            )
            .border(1.5.dp, tint, RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Aurora.Acc.copy(alpha = 0.2f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) { Text(card.kind, color = Aurora.Acc2, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.height(10.dp))
        Text(card.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(card.subtitle, color = Aurora.OnSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun SwipeBadges(alpha: Float, positive: Boolean) {
    if (alpha <= 0f) return
    Box(Modifier.fillMaxSize().padding(22.dp)) {
        Text(
            text = if (positive) "ХОЧУ" else "МИМО",
            color = if (positive) Aurora.Ok else Aurora.Error,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(if (positive) Alignment.TopStart else Alignment.TopEnd)
                .alpha(alpha)
                .rotate(if (positive) -12f else 12f)
                .border(
                    2.dp,
                    if (positive) Aurora.Ok else Aurora.Error,
                    RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ChoiceButton(label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.16f))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 26.dp, vertical = 14.dp),
    ) { Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun DeckFinished(liked: Int, onRestart: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp)
                .scale(pulse)
                .clip(RoundedCornerShape(24.dp))
                .background(Aurora.Acc.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = Aurora.Acc2, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))
        Text("Колода закончилась", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Отложено в «хочу»: " + liked + ". Новая подборка появится вместе с каталогом.",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        ChoiceButton("Пройти заново", Aurora.Acc2, onRestart)
    }
}
