package dev.takami.app.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangareader.reader.engine.novel.NovelChapter
import com.mangareader.reader.engine.novel.NovelFont
import com.mangareader.reader.engine.novel.NovelPosition
import com.mangareader.reader.engine.novel.NovelSettings
import com.mangareader.reader.engine.novel.NovelTheme
import dev.takami.app.ui.theme.Aurora
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Чтение текстовой главы.
 *
 * Отдельный экран, а не режим читалки манги: у текста нет страниц как
 * объектов, нет декодирования и нет разворотов, зато есть перевёрстка
 * при каждом изменении настроек. Общего кода почти не остаётся, а
 * ветвление внутри одной читалки дало бы условие в каждом методе — то
 * же, от чего мы отказались в постраничном режиме.
 *
 * Позиция хранится в символах и восстанавливается через абзац: при
 * смене размера шрифта абзац остаётся тем же абзацем, а пиксель
 * прокрутки — нет.
 */
@Composable
fun NovelReaderScreen(
    chapter: NovelChapter,
    settings: NovelSettings,
    initialPosition: NovelPosition,
    onPositionChange: (NovelPosition) -> Unit,
    onSettingsChange: (NovelSettings) -> Unit,
    onBack: () -> Unit,
) {
    var chromeVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val background = Color(settings.theme.backgroundArgb)
    val textColor = Color(settings.theme.textArgb)

    /*
     * Восстановление позиции идёт по абзацу, а не по прокрутке.
     * Перезапускается при смене размера шрифта и межстрочного: после
     * перевёрстки прежнее смещение в пикселях указывает в другое место,
     * и без этого читатель после каждой правки настроек оказывался бы
     * не там, где читал.
     */
    LaunchedEffect(chapter.id, settings.fontSizeSp, settings.lineHeightMultiplier, settings.horizontalPaddingDp) {
        val paragraph = chapter.paragraphAt(initialPosition.charOffset)
        if (paragraph > 0) listState.scrollToItem(paragraph)
    }

    /*
     * Сохранение позиции с задержкой: прокрутка даёт десятки событий в
     * секунду, и запись на каждое означала бы дисковую операцию на
     * каждый кадр.
     */
    LaunchedEffect(chapter.id) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(POSITION_SAVE_DELAY_MS)
            .collect { index ->
                val offset = chapter.paragraphOffsets.getOrNull(index) ?: return@collect
                onPositionChange(NovelPosition(offset))
            }
    }

    val currentOffset = chapter.paragraphOffsets
        .getOrNull(listState.firstVisibleItemIndex) ?: 0
    val progress = NovelPosition.progress(currentOffset, chapter.totalChars)

    Box(Modifier.fillMaxSize().background(background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { chromeVisible = !chromeVisible }
                .padding(horizontal = settings.horizontalPaddingDp.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 72.dp,
                bottom = 96.dp,
            ),
        ) {
            items(chapter.paragraphs) { paragraph ->
                Text(
                    text = paragraph,
                    color = textColor,
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                    fontFamily = settings.fontFamily.toCompose(),
                    textAlign = if (settings.justify) TextAlign.Justify else TextAlign.Start,
                    modifier = Modifier.padding(bottom = PARAGRAPH_GAP_DP.dp),
                )
            }
        }

        if (chromeVisible) {
            NovelTopBar(
                title = chapter.title,
                progress = progress,
                background = background,
                textColor = textColor,
                onBack = onBack,
            )
            NovelBottomBar(
                progress = progress,
                background = background,
                textColor = textColor,
                onSeek = { fraction ->
                    val offset = NovelPosition.offsetOf(fraction, chapter.totalChars)
                    val paragraph = chapter.paragraphAt(offset)
                    scope.launch { listState.scrollToItem(paragraph) }
                    onPositionChange(NovelPosition(offset))
                },
                onOpenSettings = { settingsVisible = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (settingsVisible) {
            NovelSettingsSheet(
                settings = settings,
                onChange = onSettingsChange,
                onClose = { settingsVisible = false },
            )
        }
    }
}

private const val PARAGRAPH_GAP_DP = 14
private const val POSITION_SAVE_DELAY_MS = 400L

private fun NovelFont.toCompose(): FontFamily = when (this) {
    NovelFont.SERIF -> FontFamily.Serif
    NovelFont.SANS -> FontFamily.SansSerif
    NovelFont.MONO -> FontFamily.Monospace
}

@Composable
private fun NovelTopBar(
    title: String?,
    progress: Float,
    background: Color,
    textColor: Color,
    onBack: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(background.copy(alpha = .95f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "←  " + (title ?: "Глава"),
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onBack).weight(1f, fill = false),
        )
        Text(
            "${(progress * 100).toInt()}%",
            color = textColor.copy(alpha = .6f),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun NovelBottomBar(
    progress: Float,
    background: Color,
    textColor: Color,
    onSeek: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(background.copy(alpha = .95f))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        /*
         * Слайдер по доле текста, а не по абзацам: абзацы бывают в одну
         * строку и на пол-экрана, и «5 из 20» ничего не говорит о том,
         * сколько осталось читать.
         */
        Slider(
            value = progress,
            onValueChange = onSeek,
            colors = SliderDefaults.colors(
                thumbColor = Aurora.Acc,
                activeTrackColor = Aurora.Acc,
                inactiveTrackColor = textColor.copy(alpha = .2f),
            ),
        )
        Text(
            "Настройки",
            color = Aurora.Acc2,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.End)
                .clip(RoundedCornerShape(Aurora.RadiusFull))
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun NovelSettingsSheet(
    settings: NovelSettings,
    onChange: (NovelSettings) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .6f))
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Настройки чтения", color = Aurora.OnSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

            StepperRow(
                label = "Размер шрифта",
                value = "${settings.fontSizeSp}",
                onMinus = { onChange(settings.withFontSize(settings.fontSizeSp - 1)) },
                onPlus = { onChange(settings.withFontSize(settings.fontSizeSp + 1)) },
            )
            StepperRow(
                label = "Межстрочный",
                value = String.format("%.1f", settings.lineHeightMultiplier),
                onMinus = { onChange(settings.withLineHeight(settings.lineHeightMultiplier - 0.1f)) },
                onPlus = { onChange(settings.withLineHeight(settings.lineHeightMultiplier + 0.1f)) },
            )
            StepperRow(
                label = "Поля",
                value = "${settings.horizontalPaddingDp}",
                onMinus = { onChange(settings.withPadding(settings.horizontalPaddingDp - 4)) },
                onPlus = { onChange(settings.withPadding(settings.horizontalPaddingDp + 4)) },
            )

            Text("Тема", color = Aurora.OnSurfaceVariant, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovelTheme.entries.forEach { theme ->
                    Chip(
                        label = theme.label,
                        selected = theme == settings.theme,
                        onClick = { onChange(settings.copy(theme = theme)) },
                    )
                }
            }

            Text("Шрифт", color = Aurora.OnSurfaceVariant, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovelFont.entries.forEach { font ->
                    Chip(
                        label = font.label,
                        selected = font == settings.fontFamily,
                        onClick = { onChange(settings.copy(fontFamily = font)) },
                    )
                }
            }

            Chip(
                label = "По ширине",
                selected = settings.justify,
                onClick = { onChange(settings.copy(justify = !settings.justify)) },
            )

            Spacer(Modifier.height(4.dp))
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
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Aurora.OnSurface, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepButton("−", onMinus)
            Text(value, color = Aurora.OnSurface, fontSize = 14.sp)
            StepButton("+", onPlus)
        }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Text(
        symbol,
        color = Aurora.OnSurface,
        fontSize = 18.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(Aurora.RadiusFull))
            .background(Aurora.Sub)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.Black else Aurora.OnSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(Aurora.RadiusFull))
            .background(if (selected) Aurora.Acc2 else Aurora.Sub)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
