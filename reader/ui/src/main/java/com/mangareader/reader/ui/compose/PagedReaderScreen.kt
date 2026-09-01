package com.mangareader.reader.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mangareader.reader.engine.gesture.TapZoneScheme
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.ui.view.PagedReaderView
import dev.takami.app.ui.theme.Aurora

/**
 * Compose-хост постраничного режима. Зеркало [WebtoonReaderScreen]:
 * состояния не держит, всё поднято во ViewModel, хром рисуется слоями
 * поверх в `ReaderRoot`.
 */
@Composable
fun PagedReaderScreen(
    items: List<FeedItem>,
    onViewportSettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bitmapProvider: PagedReaderView.PageBitmapProvider? = null,
    onLayoutWidth: (Int) -> Unit = {},
    onTap: () -> Unit = {},
    tapZoneScheme: TapZoneScheme = TapZoneScheme.L_SHAPE,
    isRtl: Boolean = false,
    scrollToIndex: Int? = null,
    onScrollHandled: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize().background(Aurora.Surface)) {
        AndroidView(
            factory = { context ->
                PagedReaderView(context).apply {
                    addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                        if (v.width > 0) onLayoutWidth(v.width)
                    }
                }
            },
            update = { view ->
                view.items = items
                view.bitmapProvider = bitmapProvider
                view.onViewportSettled = onViewportSettled
                view.onTap = onTap
                view.tapZoneScheme = tapZoneScheme
                view.isRtl = isRtl
                scrollToIndex?.let {
                    view.showIndex(it)
                    onScrollHandled()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
