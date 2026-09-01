package com.mangareader.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangareader.reader.engine.gesture.TapZoneScheme
import com.mangareader.reader.engine.settings.ReadingMode
import com.mangareader.reader.ui.compose.PagedReaderScreen
import com.mangareader.reader.ui.compose.WebtoonReaderScreen
import com.mangareader.reader.ui.view.PagedReaderView
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

    // Экран не гаснет, пока читалка открыта — флаг снимается вместе с
    // уходом композабла, а не только с активити: иначе он пережил бы
    // возврат в приложение.
    val view = LocalView.current
    DisposableEffect(state.settings.keepScreenOn) {
        view.keepScreenOn = state.settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier.fillMaxSize().background(Aurora.Surface)) {
        /*
         * Режим чтения выбирает вьюху, а не ветку внутри одной.
         * Постраничное чтение и непрерывная лента — разные способы
         * показа: у ленты единица измерения пиксель прокрутки, у
         * страниц — сама страница, и горизонтальный жест означает у
         * них противоположное. До этой правки постраничные режимы
         * выбирались в настройках и не были реализованы вовсе: что
         * RTL, что LTR давали ту же вертикальную ленту.
         */
        if (state.settings.readingMode.isPaged) {
            PagedReaderScreen(
                items = state.items,
                onViewportSettled = viewModel::onViewportSettled,
                bitmapProvider = object : PagedReaderView.PageBitmapProvider {
                    override fun bitmapFor(item: com.mangareader.reader.engine.layout.FeedItem.Page) =
                        viewModel.bitmapFor(item)
                },
                onLayoutWidth = viewModel::onLayoutWidth,
                onTap = viewModel::toggleChrome,
                tapZoneScheme = state.settings.tapZoneScheme,
                isRtl = state.settings.readingMode.isRtl,
                doubleSpreadEnabled = state.settings.effectiveDoubleSpread,
                scrollToIndex = state.pendingScrollIndex,
                onScrollHandled = viewModel::onScrollHandled,
            )
        } else {
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
                tapZoneScheme = state.settings.tapZoneScheme,
                isRtl = state.settings.readingMode.isRtl,
                scrollToIndex = state.pendingScrollIndex,
                onScrollHandled = viewModel::onScrollHandled,
            )
        }

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
                Spacer(Modifier.height(16.dp))
                Text(
                    "Повторить",
                    color = Aurora.OnPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(Aurora.Acc)
                        .clickable(onClick = viewModel::retry)
                        .padding(horizontal = 22.dp, vertical = 10.dp),
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
                onOpenChapters = viewModel::openChapterList,
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
                page = state.currentPage,
                total = state.totalPagesInChapter,
                isRtl = state.settings.readingMode.isRtl,
                onSeek = viewModel::seekToPage,
                onOpenSettings = viewModel::openSettings,
            )
        }

        if (state.chapterListVisible) {
            ChapterNavSheet(
                nav = state.chapterNav,
                currentChapterNumber = state.currentChapterNumber,
                onGo = viewModel::goToChapter,
                onClose = viewModel::closeChapterList,
            )
        }

        if (state.settingsVisible) {
            ReaderSettingsSheet(
                settings = state.settings,
                onChange = viewModel::updateSettings,
                onClose = viewModel::closeSettings,
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    chapterNumber: Float?,
    page: Int,
    total: Int,
    onOpenChapters: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Aurora.ScLowest.copy(alpha = .92f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Заголовок главы — он же вход в список глав: так это работает
        // в эталонной читалке, и это единственное место, откуда можно
        // уйти на предыдущую главу, не выходя из читалки.
        Text(
            (chapterNumber?.let { "Глава ${formatChapter(it)}" } ?: "Читалка") + "  ⌄",
            color = Aurora.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(Aurora.RadiusFull))
                .clickable(onClick = onOpenChapters)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Text(
            if (total > 0) "$page / $total" else "",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ReaderBottomBar(
    mode: TranslationMode,
    onModeChange: (TranslationMode) -> Unit,
    page: Int,
    total: Int,
    isRtl: Boolean,
    onSeek: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Aurora.ScLowest.copy(alpha = .92f))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (total > 1) {
            PageSlider(page = page, total = total, isRtl = isRtl, onSeek = onSeek)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TranslationMode.entries.forEach { entry ->
                    ModeChip(
                        label = entry.label(),
                        selected = entry == mode,
                        onClick = { onModeChange(entry) },
                    )
                }
            }
            Text(
                "Настройки",
                color = Aurora.Acc2,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * Слайдер страниц главы. В RTL-режиме зеркалится: ручка обязана ехать
 * туда же, куда листает тап, иначе «вперёд» означает разные стороны на
 * одном экране.
 */
@Composable
private fun PageSlider(page: Int, total: Int, isRtl: Boolean, onSeek: (Int) -> Unit) {
    // Пока пользователь тянет ручку, позиция берётся из жеста, а не из
    // состояния: иначе поток событий скролла дёргал бы ручку из-под пальца.
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shown = dragValue ?: page.toFloat()
    val last = (total - 1).coerceAtLeast(1)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${page + 1}", color = Aurora.OnSurfaceVariant, fontSize = 11.sp)
        Slider(
            value = if (isRtl) last - shown else shown,
            valueRange = 0f..last.toFloat(),
            steps = (total - 2).coerceAtLeast(0),
            onValueChange = { raw ->
                dragValue = if (isRtl) last - raw else raw
            },
            onValueChangeFinished = {
                dragValue?.let { onSeek(it.toInt()) }
                dragValue = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Aurora.Acc,
                activeTrackColor = Aurora.Acc,
                inactiveTrackColor = Aurora.Brd,
            ),
            modifier = Modifier.weight(1f),
        )
        Text("$total", color = Aurora.OnSurfaceVariant, fontSize = 11.sp)
    }
}

/**
 * Настройки тайтла (§5.1). Здесь только то, что меняет поведение чтения;
 * выбор движка перевода и языка живёт в настройках тайтла, а не тут.
 */
@Composable
private fun ReaderSettingsSheet(
    settings: com.mangareader.reader.engine.settings.ReaderSettings,
    onChange: ((com.mangareader.reader.engine.settings.ReaderSettings) -> com.mangareader.reader.engine.settings.ReaderSettings) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Aurora.ScLowest.copy(alpha = .72f))
            // Клик по подложке закрывает; indication убран, чтобы не
            // было ripple на весь экран.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = Aurora.RadiusL, topEnd = Aurora.RadiusL))
                .background(Aurora.SurfaceContainer)
                // Перехватываем клики, чтобы тап по самому листу не
                // закрывал его через подложку.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Настройки чтения", color = Aurora.OnSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

            SettingGroup("Режим чтения") {
                ReadingMode.entries.forEach { entry ->
                    ModeChip(
                        label = entry.label(),
                        selected = entry == settings.readingMode,
                        onClick = { onChange { it.copy(readingMode = entry) } },
                    )
                }
            }

            SettingGroup("Зоны нажатия") {
                TapZoneScheme.entries.forEach { entry ->
                    ModeChip(
                        label = entry.label(),
                        selected = entry == settings.tapZoneScheme,
                        onClick = { onChange { it.copy(tapZoneScheme = entry) } },
                    )
                }
            }

            SettingToggle(
                title = "Не гасить экран",
                subtitle = "Пока читалка открыта",
                checked = settings.keepScreenOn,
                onChange = { value -> onChange { it.copy(keepScreenOn = value) } },
            )
            SettingToggle(
                title = "Обрезать поля",
                subtitle = "Убирает белые края сканов",
                checked = settings.cropBordersEnabled,
                onChange = { value -> onChange { it.copy(cropBordersEnabled = value) } },
            )
            SettingToggle(
                title = "Разворот на две страницы",
                subtitle = if (settings.readingMode.isPaged) {
                    "В альбомной ориентации"
                } else {
                    "Доступно только в постраничных режимах"
                },
                checked = settings.effectiveDoubleSpread,
                enabled = settings.readingMode.isPaged,
                onChange = { value -> onChange { it.copy(tabletDoubleSpread = value) } },
            )
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Aurora.OnSurfaceVariant, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) Aurora.OnSurface else Aurora.OnSurfaceVariant,
                fontSize = 14.sp,
            )
            Text(subtitle, color = Aurora.OnSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Aurora.OnPrimary,
                checkedTrackColor = Aurora.Acc,
                uncheckedTrackColor = Aurora.Sub,
                uncheckedBorderColor = Aurora.Brd,
            ),
        )
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

private fun ReadingMode.label(): String = when (this) {
    ReadingMode.WEBTOON -> "Лента"
    ReadingMode.PAGED_RTL -> "Справа налево"
    ReadingMode.PAGED_LTR -> "Слева направо"
}

private fun TapZoneScheme.label(): String = when (this) {
    TapZoneScheme.L_SHAPE -> "Г-образные"
    TapZoneScheme.EDGES -> "По краям"
    TapZoneScheme.RIGHT_ONLY -> "Справа"
    TapZoneScheme.LEFT_ONLY -> "Слева"
    TapZoneScheme.DISABLED -> "Выключены"
}

private fun TranslationMode.label(): String = when (this) {
    TranslationMode.ORIGINAL -> "Оригинал"
    TranslationMode.OVERLAY -> "Поверх"
    TranslationMode.REPLACE -> "Заменять"
}

internal fun formatChapter(number: Float): String =
    if (number % 1f == 0f) number.toInt().toString() else number.toString()

/**
 * Переход между главами, не выходя из читалки.
 *
 * Лента бесшовна вперёд, но только вперёд и только прокруткой: попасть
 * в предыдущую главу или перепрыгнуть из середины было нельзя вообще.
 *
 * Кнопка неактивна в двух разных случаях — соседа нет и ответ ещё не
 * пришёл, — и выглядят они одинаково намеренно: нажатие, которое
 * ничего не делает, хуже кнопки, которая честно ждёт.
 */
@Composable
private fun ChapterNavSheet(
    nav: ChapterNav,
    currentChapterNumber: Float?,
    onGo: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Aurora.ScLowest.copy(alpha = .72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = Aurora.RadiusL, topEnd = Aurora.RadiusL))
                .background(Aurora.SurfaceContainer)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                currentChapterNumber?.let { "Глава ${formatChapter(it)}" } ?: "Главы",
                color = Aurora.OnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            ChapterNavRow(
                label = "Предыдущая глава",
                targetId = nav.prevId,
                resolved = nav.resolved,
                onGo = onGo,
            )
            ChapterNavRow(
                label = "Следующая глава",
                targetId = nav.nextId,
                resolved = nav.resolved,
                onGo = onGo,
            )
            Text(
                "Закрыть",
                color = Aurora.Acc2,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ChapterNavRow(
    label: String,
    targetId: String?,
    resolved: Boolean,
    onGo: (String) -> Unit,
) {
    val enabled = resolved && targetId != null
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Aurora.RadiusS))
            .background(Aurora.Sub)
            .let { if (enabled) it.clickable { onGo(targetId!!) } else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = if (enabled) Aurora.OnSurface else Aurora.OnSurfaceVariant,
            fontSize = 14.sp,
        )
        Text(
            when {
                !resolved -> "…"
                targetId == null -> "нет"
                else -> "›"
            },
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}
