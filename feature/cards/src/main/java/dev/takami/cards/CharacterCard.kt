package dev.takami.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

/**
 * Карточка персонажа по макету `kit/Title.jsx` + `kit-1.css` (`.c-card`).
 *
 * Портрет 3:4, радиус 14 — как в макете. Обложка персонажа в макете это не
 * фотография, а КРУПНЫЙ ИНИЦИАЛ, посаженный в правый нижний угол с выносом
 * за край: имя читается даже когда картинки нет, а нет её всегда — источника
 * по персонажам у нас не существует. Поэтому глиф здесь не «плейсхолдер до
 * загрузки», а основное оформление карточки.
 */
@Composable
fun CharacterCard(
    data: CharacterCardData,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    width: androidx.compose.ui.unit.Dp = 96.dp,
    onClick: (() -> Unit)? = null,
) {
    val main = data.role == CharacterRole.Main
    Column(
        modifier
            .width(width)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(PORTRAIT_RADIUS))
                .border(
                    1.dp,
                    // Главного героя макет выделяет самой карточкой, а не
                    // подписью: акцентная рамка вместо обычной.
                    if (main) Aurora.Acc.copy(alpha = 0.55f) else Aurora.Brd,
                    RoundedCornerShape(PORTRAIT_RADIUS),
                ),
        ) {
            CardImage(
                id = data.id,
                fallbackText = "",
                imageUrl = data.imageUrl,
                loader = loader,
                targetWidthPx = PORTRAIT_TARGET_WIDTH_PX,
                modifier = Modifier.fillMaxSize(),
            )

            /*
             * Оверлей из макета: блик в левом верхнем углу и затемнение к
             * низу. Без затемнения подпись в углу теряется на светлом
             * градиенте, а без блика карточка выглядит плоской заливкой.
             */
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(0.25f, 0.18f),
                            radius = Float.POSITIVE_INFINITY,
                        )
                    )
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f),
                        )
                    )
            )

            // Крупный инициал в правом нижнем углу с выносом за край —
            // ровно как `.c-glyph` (right: -6px, bottom: -18px).
            Text(
                CardText.glyph(data.name),
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 58.sp,
                lineHeight = 58.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 12.dp),
            )

            // Имя на языке оригинала — вертикально в правом верхнем углу.
            // Compose не умеет writing-mode, поэтому столбик по символам:
            // японская подпись в макете и так набрана в одну колонку.
            data.nativeName?.takeIf { it.isNotBlank() }?.let { native ->
                Column(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    native.take(NATIVE_GLYPH_LIMIT).forEach { ch ->
                        Text(
                            ch.toString(),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            /*
             * «Главный» — подпись-пилюля в левом верхнем углу. В `kit-1.css`
             * на этом месте была точка, но `patches.css` возвращает текст:
             * патч новее, поэтому беру его. Второстепенных макет здесь не
             * подписывает вовсе — роль уходит под карточку.
             */
            if (main) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(Aurora.Surface.copy(alpha = 0.7f))
                        .border(
                            1.dp,
                            Aurora.Acc.copy(alpha = 0.4f),
                            RoundedCornerShape(Aurora.RadiusFull),
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        "Главный",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        // Подписи под карточкой прижаты влево и по одной строке каждая:
        // в макете `-webkit-line-clamp: 1`, и это не мелочь — второй строкой
        // ряд карточек превращается в неровную гребёнку.
        Text(
            data.name,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
        CardText.roleLabel(data.role)?.let { role ->
            Text(
                role,
                // Роль главного — акцентным цветом (`.c-card.main .c-ro`).
                color = if (main) Aurora.Acc2 else Aurora.OnSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Сэйю в ряду карточек макет не показывает — он на экране персонажа.
    }
}

/**
 * Ряд персонажей для страницы тайтла.
 *
 * Источника данных по персонажам в проекте НЕТ — ни автопарсер, ни локальные
 * файлы их не отдают. Поэтому при пустом списке ряд показывает честное
 * состояние, а не моки: подставить сюда пятерых вымышленных персонажей
 * значило бы добавить ещё один случай «выглядит готовым и не работает».
 */
@Composable
fun CharacterRail(
    characters: List<CharacterCardData>,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    title: String = "Персонажи",
    onOpen: ((CharacterCardData) -> Unit)? = null,
    onOpenAll: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        SectionHead(
            title = title,
            // «Все N ›» из макета — только когда есть что открывать.
            action = if (characters.isNotEmpty() && onOpenAll != null) {
                "Все ${characters.size} ›"
            } else {
                null
            },
            onAction = onOpenAll,
        )
        Spacer(Modifier.height(10.dp))
        if (characters.isEmpty()) {
            NoCharacters(Modifier.padding(horizontal = 16.dp))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(characters, key = { it.id }) { character ->
                    CharacterCard(
                        data = character,
                        loader = loader,
                        onClick = onOpen?.let { open -> { open(character) } },
                    )
                }
            }
        }
    }
}

/** Заголовок секции с необязательной ссылкой справа — `.sechead` из макета. */
@Composable
internal fun SectionHead(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            // Подпись секции из макета — пояснение, а не украшение: пустая
            // строка на её месте сдвигала бы ряд карточек вниз.
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Aurora.OnSurfaceVariant, fontSize = 11.sp)
            }
        }
        if (action != null && onAction != null) {
            Text(
                action,
                color = Aurora.Acc,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onAction() },
            )
        }
    }
}

/** Пустое состояние: объясняет, почему пусто, вместо «нет данных». */
@Composable
private fun NoCharacters(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Aurora.RadiusM))
            .background(Aurora.SurfaceContainer)
            .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM))
            .padding(16.dp),
    ) {
        Text(
            "Персонажи пока не подключены",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Ни один из источников не отдаёт список персонажей. Появятся, " +
                "когда источник начнёт их отдавать.",
            color = Aurora.OnSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

private val PORTRAIT_RADIUS = 14.dp
private const val PORTRAIT_TARGET_WIDTH_PX = 300

/**
 * Сколько символов оригинального имени влезает в вертикальный столбик:
 * `max-height: calc(100% - 12px)` при кегле 9px на портрете 96×128.
 */
private const val NATIVE_GLYPH_LIMIT = 10
