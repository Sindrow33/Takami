package dev.takami.swipes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
fun SwipesScreen(
    modifier: Modifier = Modifier,
    source: SwipeSource = NoSwipeSource,
    decisions: SwipeDecisionStore = NoDecisionStore,
) {
    val context = LocalContext.current
    val coverLoader = remember { CoverLoader(java.io.File(context.cacheDir, "covers")) }
    // null — ещё загружаем; пустой список — данных нет, и это нормальное
    // состояние, а не ошибка: пока сайт не разобран, подбирать нечего.
    var deck by remember { mutableStateOf<DeckState?>(null) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val dragX = remember { Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        val all = source.cards()
        val decided = decisions.decidedIds()
        deck = DeckState(SwipeMath.filterUndecided(all, decided))
    }

    fun commit(direction: SwipeDirection) {
        val current = deck ?: return
        if (direction == SwipeDirection.None) {
            scope.launch { dragX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 420f)) }
            return
        }
        val card = current.current ?: return
        scope.launch {
            dragX.animateTo(SwipeMath.flyAwayX(direction, widthPx), tween(220))
            deck = current.apply(direction)
            dragX.snapTo(0f)
            // Решение записываем ПОСЛЕ анимации, но до следующего свайпа:
            // иначе быстрая серия свайпов теряет часть записей.
            decisions.record(card.id, direction)
            if (direction == SwipeDirection.Like) source.like(card)
        }
    }

    val current = deck

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
            if (current != null && !current.isFinished) {
                Text(
                    "осталось " + current.remaining,
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
            when {
                current == null -> Unit // первая загрузка, экран пустой
                current.cards.isEmpty() -> NothingToSwipe()
                current.isFinished -> DeckFinished(
                    liked = current.liked.size,
                    onRestart = {
                        // «Пройти заново» очищает решения: иначе колода
                        // после очистки собирается снова пустой.
                        scope.launch {
                            decisions.clear()
                            reloadKey++
                        }
                    },
                )
                else -> {
                    current.next?.let { under ->
                        DeckCardView(
                            card = under,
                            modifier = Modifier
                                .scale(SwipeMath.underCardScale(dragX.value, widthPx))
                                .alpha(0.55f),
                            coverLoader = coverLoader,
                        )
                    }
                    current.current?.let { top ->
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
                            DeckCardView(card = top, glow = progress, coverLoader = coverLoader)
                            SwipeBadges(
                                alpha = SwipeMath.badgeAlpha(dragX.value, widthPx),
                                positive = progress > 0f,
                            )
                        }
                    }
                }
            }
        }

        if (current != null && !current.isFinished && current.cards.isNotEmpty()) {
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

/**
 * Пустое состояние. Говорит, ЧТО сделать, а не «список пуст»: пока сайт не
 * разобран и локальных файлов нет, подбирать действительно нечего, и
 * пользователь должен понимать, что это не поломка.
 */
@Composable
private fun NothingToSwipe() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Aurora.SurfaceContainer),
            contentAlignment = Alignment.Center,
        ) { Text("∅", color = Aurora.OnSurfaceVariant, fontSize = 26.sp) }
        Spacer(Modifier.height(16.dp))
        Text("Подбирать пока нечего", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Карточки берутся из вашей библиотеки и из разобранных сайтов. " +
                "Добавьте папку с файлами или разберите сайт в Настройках.",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeckCardView(
    card: DeckCard,
    modifier: Modifier = Modifier,
    glow: Float = 0f,
    coverLoader: CoverLoader? = null,
) {
    val tint = when {
        glow > 0f -> Aurora.Ok.copy(alpha = 0.35f * glow)
        glow < 0f -> Aurora.Error.copy(alpha = 0.35f * -glow)
        else -> Aurora.Outline.copy(alpha = 0.5f)
    }

    var cover by remember(card.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val url = card.coverUrl
    LaunchedEffect(card.id, url) {
        if (url != null && coverLoader != null) {
            cover = coverLoader.load(url, COVER_TARGET_WIDTH_PX)
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Aurora.SurfaceContainer, Aurora.ScLow)
                )
            )
            .border(1.5.dp, tint, RoundedCornerShape(24.dp)),
    ) {
        val bitmap = cover
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Затемнение снизу: без него белый текст на светлой обложке
            // не читается вообще.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.85f),
                        )
                    )
            )
        }
        CardText(card, Modifier.align(Alignment.BottomStart))
    }
}

/** Целевая ширина обложки в пикселях — карточка занимает почти всю ширину экрана. */
private const val COVER_TARGET_WIDTH_PX = 720

@Composable
private fun CardText(card: DeckCard, modifier: Modifier) {
    Column(
        modifier.padding(22.dp),
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
        if (card.subtitle.isNotBlank()) {
            Text(card.subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        }
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
