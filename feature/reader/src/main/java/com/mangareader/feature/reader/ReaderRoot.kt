package com.mangareader.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangareader.reader.ui.compose.WebtoonReaderScreen
import com.mangareader.reader.ui.view.WebtoonFeedView
import com.mangareader.translate.api.TranslationMode
import dev.takami.app.ui.theme.Aurora

/**
 * Корень экрана читалки: лента + хром поверх неё.
 *
 * По умолчанию на экране нет ничего, кроме страницы (§5.1) — топбар и
 * нижняя панель появляются по тапу и уезжают тем же движением. Цвета,
 * радиусы и кривые — токены Aurora, своих констант тут нет.
 */
@Composable
fun ReaderRoot(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize().background(Aurora.Surface)) {
        WebtoonReaderScreen(
            items = state.items,
            translationMode = state.translationMode,
            onViewportSettled = viewModel::onViewportSettled,
            bitmapProvider = object : WebtoonFeedView.PageBitmapProvider {
                override fun bitmapFor(item: com.mangareader.reader.engine.layout.FeedItem.Page) =
                    viewModel.bitmapFor(item)
            },
            onLayoutWidth = viewModel::onLayoutWidth,
            onTap = viewModel::toggleChrome,
        )

        if (state.loading && state.items.isEmpty() && state.error == null) {
            Text(
                "Загрузка главы…",
                color = Aurora.OnSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        state.error?.let { message ->
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Глава не открылась", color = Aurora.OnSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    message,
                    color = Aurora.OnSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = slideInVertically(tween(Aurora.DurMid, easing = Aurora.EaseOut)) { -it } +
                fadeIn(tween(Aurora.DurMid)),
            exit = slideOutVertically(tween(Aurora.DurFast)) { -it } + fadeOut(tween(Aurora.DurFast)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(
                chapterNumber = state.currentChapterNumber,
                page = state.currentPage + 1,
                total = state.totalPagesInChapter,
            )
        }

        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = slideInVertically(tween(Aurora.DurMid, easing = Aurora.EaseOut)) { it } +
                fadeIn(tween(Aurora.DurMid)),
            exit = slideOutVertically(tween(Aurora.DurFast)) { it } + fadeOut(tween(Aurora.DurFast)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderBottomBar(
                mode = state.translationMode,
                onModeChange = viewModel::setTranslationMode,
            )
        }
    }
}

@Composable
private fun ReaderTopBar(chapterNumber: Float?, page: Int, total: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Aurora.ScLowest.copy(alpha = .92f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            chapterNumber?.let { "Глава ${formatChapter(it)}" } ?: "Читалка",
            color = Aurora.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (total > 0) "$page / $total" else "",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ReaderBottomBar(mode: TranslationMode, onModeChange: (TranslationMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Aurora.ScLowest.copy(alpha = .92f))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TranslationMode.entries.forEach { entry ->
            ModeChip(
                label = entry.label(),
                selected = entry == mode,
                onClick = { onModeChange(entry) },
            )
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(Aurora.RadiusFull))
            .background(if (selected) Aurora.Acc else Aurora.Sub)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) Aurora.OnPrimary else Aurora.OnSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun TranslationMode.label(): String = when (this) {
    TranslationMode.ORIGINAL -> "Оригинал"
    TranslationMode.OVERLAY -> "Поверх"
    TranslationMode.REPLACE -> "Заменять"
}

internal fun formatChapter(number: Float): String =
    if (number % 1f == 0f) number.toInt().toString() else number.toString()
