package dev.takami.app.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import dev.takami.app.ui.theme.Aurora

/**
 * «Новости аниме» — блок, который был в макете, но в сборку не попал:
 * между быстрыми действиями и рельсой «Продолжить» зияла пустота.
 * Карусель с привязкой к карточке и точками-индикаторами.
 */
@Composable
fun NewsRail(
    items: List<NewsItem> = newsFeed,
    onOpen: (NewsItem) -> Unit = {},
    // null — ссылки «Все ›» не будет: экрана всех новостей пока нет, а
    // кнопка, которая ничего не открывает, читается как поломка.
    onOpenAll: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val state = rememberLazyListState()

    // Активная точка — та карточка, что занимает большую часть экрана.
    val activeIndex by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - center)
            }?.index ?: 0
        }
    }

    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        SectionHeader(
            title = "Новости аниме",
            subtitle = "что происходит в индустрии",
            onOpenAll = onOpenAll,
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { NewsCard(it, onOpen) }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            items.take(MAX_DOTS).forEachIndexed { i, _ ->
                val active = i == activeIndex
                val width by animateDpAsState(if (active) 18.dp else 6.dp, label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(if (active) Aurora.Acc else Aurora.BrdEm)
                )
            }
        }
    }
}

/** Больше десятка точек читаются как рябь, а не как индикатор. */
private const val MAX_DOTS = 10

/** Заголовок секции с ссылкой «Все ›» — общий для новостей и рельс. */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onOpenAll: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = Aurora.OnSurfaceVariant, fontSize = 11.5.sp)
            }
        }
        if (onOpenAll != null) {
            Text(
                "Все ›",
                color = Aurora.Acc2,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .clickable(onClick = onOpenAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun NewsCard(item: NewsItem, onOpen: (NewsItem) -> Unit) {
    Column(
        Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(Aurora.RadiusL))
            .background(Aurora.SurfaceContainer)
            .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusL))
            .clickable { onOpen(item) }
    ) {
        // Обложка: пока градиент по тону новости — реальные картинки
        // приедут вместе с источником новостей.
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.linearGradient(listOf(item.tone.coverFrom, item.tone.coverTo))
                )
        ) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .background(item.tone.accent.copy(alpha = .18f))
                    .border(1.dp, item.tone.accent.copy(alpha = .45f), RoundedCornerShape(Aurora.RadiusFull))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    item.category.uppercase(),
                    color = item.tone.accent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp,
                )
            }
        }

        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                item.subtitle,
                color = Aurora.OnSurfaceVariant,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.source,
                    color = Aurora.OnSurfaceVariant.copy(alpha = .8f),
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                // Возраст рисуется только когда он известен: точка с
                // пустотой после неё выглядит как потерянный текст.
                if (item.age.isNotEmpty()) {
                    Text(" · ", color = Aurora.OnSurfaceVariant.copy(alpha = .5f), fontSize = 10.5.sp)
                    Text(item.age, color = Aurora.OnSurfaceVariant.copy(alpha = .8f), fontSize = 10.5.sp)
                }
            }
        }
    }
}
