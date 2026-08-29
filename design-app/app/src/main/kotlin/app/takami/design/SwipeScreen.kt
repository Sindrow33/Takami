package app.takami.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.launch

private data class DeckCard(val uid: Int, val item: FakeTitle)

private fun buildDeck(): List<DeckCard> {
    var uid = 0
    return (0 until 3).flatMap { pass ->
        Fake.titles.filter { !it.broken }.map { DeckCard(uid++, it) }
    }
}

@Composable
fun SwipeScreen(onBack: () -> Unit = {}, onOpen: (FakeTitle) -> Unit = {}) {
    val deck = remember { mutableStateListOf<DeckCard>().apply { addAll(buildDeck()) } }
    var liked by rememberSaveable { mutableIntStateOf(0) }
    var skipped by rememberSaveable { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(Dim.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹", fontSize = 24.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(horizontal = Dim.s3),
            )
            Text("Подбор", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "♥ $liked · ✕ $skipped", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = Dim.s4),
            )
        }
        Text(
            "Тяните карточку: вправо — в библиотеку, влево — пропустить.",
            fontSize = 12.sp, lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Dim.s4, 0.dp, Dim.s4, Dim.s3),
        )

        Box(
            Modifier.fillMaxWidth().weight(1f).padding(Dim.s4),
            contentAlignment = Alignment.Center,
        ) {
            if (deck.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Карточки закончились", fontSize = 15.sp)
                    Text(
                        "Загляните позже — подборка обновляется",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Box(
                        Modifier.padding(top = Dim.s4)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { deck.addAll(buildDeck()); liked = 0; skipped = 0 }
                            .padding(horizontal = Dim.s6, vertical = Dim.s3)
                    ) { Text("Начать заново", fontSize = 13.sp, color = Color.White) }
                }
            } else {
                // подложки: следующие две карточки, чуть меньше и ниже
                deck.take(3).drop(1).reversed().forEachIndexed { i, c ->
                    val depth = (2 - i)
                    Box(
                        Modifier
                            .fillMaxWidth(1f - depth * 0.04f)
                            .fillMaxHeight(0.92f - depth * 0.03f)
                            .offset(y = (depth * 10).dp)
                            .clip(RoundedCornerShape(Dim.rL))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                }
                val top = deck.first()
                key(top.uid) {
                    TopCard(
                        card = top,
                        onDismiss = { right ->
                            if (right) liked++ else skipped++
                            lastAction = if (right) "В библиотеке: ${top.item.name}" else null
                            deck.removeAt(0)
                        },
                        onTap = { onOpen(top.item) },
                    )
                }
            }
        }

        if (deck.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s6),
                horizontalArrangement = Arrangement.spacedBy(Dim.s4, Alignment.CenterHorizontally),
            ) {
                RoundBtn("✕", MaterialTheme.colorScheme.surfaceVariant) {
                    skipped++; deck.removeAt(0)
                }
                RoundBtn("ⓘ", MaterialTheme.colorScheme.surfaceVariant) {
                    onOpen(deck.first().item)
                }
                RoundBtn("♥", MaterialTheme.colorScheme.primary) {
                    liked++; deck.removeAt(0)
                }
            }
        }
        lastAction?.let {
            Text(
                it, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s4),
            )
        }
    }
}

@Composable
private fun TopCard(card: DeckCard, onDismiss: (Boolean) -> Unit, onTap: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val threshold = 320f

    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = offsetX.value / 28f
            }
            .clip(RoundedCornerShape(Dim.rL))
            .background(card.item.cover)
            .pointerInput(card.uid) {
                detectDragGestures(
                    onDragEnd = {
                        val x = offsetX.value
                        if (abs(x) > threshold) {
                            val target = if (x > 0) 1600f else -1600f
                            scope.launch {
                                offsetX.animateTo(target, tween(220))
                                onDismiss(x > 0)
                            }
                        } else {
                            scope.launch { offsetX.animateTo(0f, tween(200)) }
                            scope.launch { offsetY.animateTo(0f, tween(200)) }
                        }
                    },
                    onDrag = { _, drag ->
                        scope.launch { offsetX.snapTo(offsetX.value + drag.x) }
                        scope.launch { offsetY.snapTo(offsetY.value + drag.y * 0.35f) }
                    },
                )
            }
            .clickable(onClick = onTap),
    ) {
        // метки решения
        val x = offsetX.value
        if (x > 40f) Stamp("В БИБЛИОТЕКУ", Color(0xFF3DD68C), Alignment.TopStart, (x / threshold))
        if (x < -40f) Stamp("ПРОПУСК", Color(0xFFF87171), Alignment.TopEnd, (-x / threshold))

        Column(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(Dim.s4),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                card.item.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White, lineHeight = 23.sp,
            )
            Text(
                "${card.item.fmt.title} · ${card.item.source} · ★ 8.4",
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dim.s1)) {
                listOf("Экшен", "Фэнтези", "Сёнэн").forEach { g ->
                    Box(
                        Modifier.clip(RoundedCornerShape(Dim.rS))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) { Text(g, fontSize = 10.sp, color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun Stamp(text: String, color: Color, align: Alignment, alpha: Float) {
    Box(
        Modifier.fillMaxSize().padding(Dim.s4),
        contentAlignment = align,
    ) {
        Box(
            Modifier
                .graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) }
                .clip(RoundedCornerShape(Dim.rS))
                .background(color.copy(alpha = 0.22f))
                .padding(horizontal = Dim.s3, vertical = 6.dp)
        ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color) }
    }
}

@Composable
private fun RoundBtn(glyph: String, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, fontSize = 20.sp, color = Color.White) }
}

@Preview(name = "Swipe", showBackground = true, heightDp = 900)
@Composable
private fun PreviewSwipe() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { SwipeScreen() }
}
