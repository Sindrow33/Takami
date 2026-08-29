package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SurfaceLevel { Subtle, Glass, Strong }

@Composable
fun AuroraSurface(
    modifier: Modifier = Modifier,
    level: SurfaceLevel = SurfaceLevel.Glass,
    radius: androidx.compose.ui.unit.Dp = Dim.rL,
    content: @Composable BoxScope.() -> Unit,
) {
    val a = LocalAurora.current
    val shape = RoundedCornerShape(radius)
    val bg = when (level) {
        SurfaceLevel.Subtle -> a.glass.copy(alpha = if (a.isDark) 0.04f else 1f)
        SurfaceLevel.Glass -> a.glass
        SurfaceLevel.Strong -> MaterialTheme.colorScheme.surfaceContainer
    }
    val brd = if (level == SurfaceLevel.Strong) a.borderStrong else a.border
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, brd, shape),
        content = content,
    )
}

/** Тонкая полоса прогресса, как .progress в прототипе. */
@Composable
fun ProgressLine(percent: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(Dim.s4, Dim.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

/** Карточка тайтла для рельсы: обложка, бейдж, прогресс, название. */
@Composable
fun TitleCard(item: FakeTitle, onClick: () -> Unit = {}) {
    Column(Modifier.width(Dim.cover).clickable(onClick = onClick)) {
        Box(
            Modifier
                .width(Dim.cover)
                .height(Dim.cover * 3 / 2)
                .clip(RoundedCornerShape(Dim.rM))
                .background(item.cover)
        ) {
            item.badge?.let { b ->
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(Dim.rS))
                        .background(
                            if (item.broken) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) { Text(b, fontSize = 10.sp, color = Color.White) }
            }
            if (item.progress in 1..99) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(6.dp)
                        .fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.progress / 100f)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
        Text(
            item.name, fontSize = 12.sp, lineHeight = 15.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Плитка быстрого действия с подписью-счётчиком. */
@Composable
fun QuickTile(icon: String, label: String, hint: String, onClick: () -> Unit = {}) {
    AuroraSurface(
        Modifier.width(0.dp).height(74.dp),
        level = SurfaceLevel.Subtle,
        radius = Dim.rM,
    ) {}
}
