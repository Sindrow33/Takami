package dev.takami.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

/**
 * Сетка карточек тайтлов — для библиотеки и результатов разбора сайта.
 *
 * Колонки задаются минимальной шириной, а не числом: на узком экране три
 * карточки превращаются в нечитаемые полоски, а на планшете две оставляют
 * половину экрана пустой.
 */
@Composable
fun TitleGrid(
    titles: List<TitleCardData>,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    emptyTitle: String = "Здесь пока пусто",
    emptyHint: String = "Добавьте папку с файлами или разберите сайт в Настройках.",
    onOpen: ((TitleCardData) -> Unit)? = null,
) {
    if (titles.isEmpty()) {
        EmptyBlock(emptyTitle, emptyHint, modifier.padding(16.dp))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(titles, key = { it.id }) { title ->
            TitleCard(
                data = title,
                loader = loader,
                // Ширину задаёт колонка сетки, а не карточка: иначе
                // фиксированные 104dp оставляют дырку справа на планшете.
                width = null,
                style = TitleCardStyle.Grid,
                onClick = onOpen?.let { open -> { open(title) } },
            )
        }
    }
}

/** Горизонтальный ряд карточек — для подборок на главной. */
@Composable
fun TitleRail(
    title: String,
    titles: List<TitleCardData>,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    subtitle: String? = null,
    onOpen: ((TitleCardData) -> Unit)? = null,
    onOpenAll: (() -> Unit)? = null,
) {
    // Пустой ряд не рисуем совсем: заголовок подборки без содержимого
    // выглядит как сломанная загрузка.
    if (titles.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        SectionHead(
            title = title,
            subtitle = subtitle,
            // «Все ›» из макета — только когда есть куда вести.
            action = if (onOpenAll != null) "Все ›" else null,
            onAction = onOpenAll,
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(titles, key = { it.id }) { item ->
                TitleCard(
                    data = item,
                    loader = loader,
                    onClick = onOpen?.let { open -> { open(item) } },
                )
            }
        }
    }
}

@Composable
internal fun EmptyBlock(title: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Aurora.RadiusM))
            .background(Aurora.SurfaceContainer)
            .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM))
            .padding(16.dp),
    ) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(hint, color = Aurora.OnSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
    }
}
