package dev.takami.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

/**
 * Карточка тайтла: обложка 2:3, тип контента, бейдж непрочитанного,
 * полоска прогресса, название и рейтинг.
 *
 * Пропорция 2:3 фиксирована, а не задана высотой в dp: обложки приходят
 * разных размеров, и при фиксированной высоте сетка из карточек
 * расползается по вертикали.
 */
@Composable
fun TitleCard(
    data: TitleCardData,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    width: androidx.compose.ui.unit.Dp = 112.dp,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .width(width)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(Aurora.RadiusM))
                .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM)),
        ) {
            CardImage(
                id = data.id,
                fallbackText = data.title,
                imageUrl = data.coverUrl,
                loader = loader,
                targetWidthPx = COVER_TARGET_WIDTH_PX,
                modifier = Modifier.fillMaxSize(),
            )

            // Метка типа контента цветом из токенов — так тип читается
            // без текста, когда карточка мелкая.
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(Aurora.RadiusS))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    CardText.kindLabel(data.kind),
                    color = colorFor(data.kind),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            CardText.badgeLabel(data.badgeCount)?.let { badge ->
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(Aurora.Acc)
                        // Размер по содержимому, а не жёсткий круг: в круге
                        // двузначное число прижимается к краю — ту же ошибку
                        // уже ловили на бейдже календаря.
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (CardText.showProgress(data.progress)) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(CardText.clampProgress(data.progress))
                            .height(3.dp)
                            .background(colorFor(data.kind)),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            data.title,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            data.rating?.let {
                Text("★ $it", color = Aurora.Warn, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
            }
            data.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = Aurora.OnSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Широкая карточка для списка: обложка слева, текст справа. Нужна там, где
 * вертикальная сетка неуместна — в библиотеке списком и в результатах поиска.
 */
@Composable
fun TitleRowCard(
    data: TitleCardData,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Aurora.RadiusM))
            .background(Aurora.SurfaceContainer)
            .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(52.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(Aurora.RadiusS)),
        ) {
            CardImage(
                id = data.id,
                fallbackText = data.title,
                imageUrl = data.coverUrl,
                loader = loader,
                targetWidthPx = ROW_COVER_TARGET_WIDTH_PX,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                data.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    CardText.kindLabel(data.kind),
                    color = colorFor(data.kind),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                data.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = Aurora.OnSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                }
            }
            if (CardText.showProgress(data.progress)) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Aurora.Outline.copy(alpha = 0.4f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(CardText.clampProgress(data.progress))
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colorFor(data.kind)),
                    )
                }
            }
        }
        CardText.badgeLabel(data.badgeCount)?.let { badge ->
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .background(Aurora.Acc)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Цвет типа контента из токенов: аниме, манга и ранобэ различаются им во всём приложении. */
internal fun colorFor(kind: ContentKind): Color = when (kind) {
    ContentKind.Anime -> Aurora.TypeAnime
    ContentKind.Manga -> Aurora.TypeManga
    ContentKind.Novel -> Aurora.TypeNovel
}

private const val COVER_TARGET_WIDTH_PX = 360
private const val ROW_COVER_TARGET_WIDTH_PX = 180
