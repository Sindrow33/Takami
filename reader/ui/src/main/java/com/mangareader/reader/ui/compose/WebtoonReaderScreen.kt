package com.mangareader.reader.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mangareader.reader.engine.gesture.TapZoneScheme
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.ui.view.WebtoonFeedView
import com.mangareader.translate.api.TranslationMode
import dev.takami.app.ui.theme.Aurora

/**
 * Compose-хост для [WebtoonFeedView] (§6: «кастомная View внутри
 * AndroidView, всё вокруг — в Compose»).
 *
 * Композабл намеренно тонкий: собственного состояния чтения не держит.
 * [items], [translationMode] и битмапы подняты во ViewModel из
 * `:feature:reader`; хром (топбар, слайдер, настройки) слоями рисуется
 * над ним в [ReaderRoot], а не здесь.
 *
 * Фон — `Aurora.Surface`: он же виден в момент, когда страница ещё не
 * декодирована и лента рисует плейсхолдер.
 */
@Composable
fun WebtoonReaderScreen(
    items: List<FeedItem>,
    translationMode: TranslationMode,
    onViewportSettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bitmapProvider: WebtoonFeedView.PageBitmapProvider? = null,
    onLayoutWidth: (Int) -> Unit = {},
    onTap: () -> Unit = {},
    tapZoneScheme: TapZoneScheme = TapZoneScheme.L_SHAPE,
    isRtl: Boolean = false,
    /** Запрос перехода на элемент ленты; сбрасывается вызовом [onScrollHandled]. */
    scrollToIndex: Int? = null,
    onScrollHandled: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize().background(Aurora.Surface)) {
        AndroidView(
            factory = { context ->
                WebtoonFeedView(context).apply {
                    addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                        if (v.width > 0) onLayoutWidth(v.width)
                    }
                }
            },
            update = { view ->
                view.items = items
                view.translationMode = translationMode
                view.bitmapProvider = bitmapProvider
                view.onViewportSettled = onViewportSettled
                view.onTap = onTap
                view.tapZoneScheme = tapZoneScheme
                view.isRtl = isRtl
                scrollToIndex?.let {
                    view.scrollToIndex(it)
                    onScrollHandled()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
