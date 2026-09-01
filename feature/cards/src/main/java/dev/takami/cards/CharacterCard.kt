package dev.takami.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

/**
 * Карточка персонажа: портрет, имя, роль, актёр озвучки.
 *
 * Портрет 3:4, а не квадрат: персонажей рисуют по грудь, и квадрат режет
 * либо лицо, либо оставляет пустое поле сверху.
 */
@Composable
fun CharacterCard(
    data: CharacterCardData,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    width: androidx.compose.ui.unit.Dp = 96.dp,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .width(width)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(Aurora.RadiusM))
                .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM)),
        ) {
            CardImage(
                id = data.id,
                fallbackText = data.name,
                imageUrl = data.imageUrl,
                loader = loader,
                targetWidthPx = PORTRAIT_TARGET_WIDTH_PX,
                modifier = Modifier.fillMaxSize(),
            )
            CardText.roleLabel(data.role)?.let { role ->
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Text(
                        role,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            data.name,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
        )
        // Имя на языке оригинала и актёр — по одной строке каждое: две
        // строки под именем превращают ряд карточек в неровную гребёнку.
        data.nativeName?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = Aurora.OnSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        data.voiceActor?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = Aurora.Acc2,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Ряд персонажей для страницы тайтла.
 *
 * Источника данных по персонажам в проекте НЕТ — ни автопарсер, ни локальные
 * файлы их не отдают. Поэтому при пустом списке ряд показывает честное
 * состояние, а не моки: сегодня как раз выпиливаем всё выдуманное, и
 * подставить сюда пятерых вымышленных персонажей значило бы добавить седьмой
 * случай «выглядит готовым и не работает».
 */
@Composable
fun CharacterRail(
    characters: List<CharacterCardData>,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    title: String = "Персонажи",
    onOpen: ((CharacterCardData) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        if (characters.isEmpty()) {
            NoCharacters(Modifier.padding(horizontal = 16.dp))
        } else {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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

private const val PORTRAIT_TARGET_WIDTH_PX = 300
