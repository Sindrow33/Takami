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
import androidx.compose.foundation.layout.size
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
 * Как рисовать карточку. Макет даёт ДВЕ разные карточки под один и тот же
 * тайтл, и это не оформительский каприз:
 *
 * - [Rail] — ряд на главной (`.hm3-card` в `kit-3.css`): прогресс лежит
 *   ПОВЕРХ обложки, под названием только рейтинг. Ряд листается, и лишняя
 *   строка под каждой карточкой съедала бы высоту у всего экрана.
 * - [Grid] — сетка библиотеки (`.card` в `kit-1.css`): прогресс полоской ПОД
 *   обложкой, плюс строка-подпись с числом глав. В библиотеке карточку
 *   разглядывают, а не проматывают.
 */
enum class TitleCardStyle { Rail, Grid }

/**
 * Карточка тайтла по макету. Обложка с радиусом 14 (рельса) или 12 (сетка),
 * метка типа чипом в левом верхнем углу, бейдж непрочитанного справа.
 *
 * Пропорция 2:3 задана соотношением, а не высотой в dp: обложки приходят
 * разных размеров, и при фиксированной высоте сетка расползается по
 * вертикали.
 */
@Composable
fun TitleCard(
    data: TitleCardData,
    modifier: Modifier = Modifier,
    loader: ImageLoader? = null,
    /** `null` — ширину задаёт родитель (колонка сетки). */
    width: androidx.compose.ui.unit.Dp? = 118.dp,
    style: TitleCardStyle = TitleCardStyle.Rail,
    onClick: (() -> Unit)? = null,
) {
    val radius = if (style == TitleCardStyle.Rail) 14.dp else Aurora.RadiusM
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(COVER_RATIO)
                .clip(RoundedCornerShape(radius))
                .border(1.dp, Aurora.Brd, RoundedCornerShape(radius)),
        ) {
            CardImage(
                id = data.id,
                fallbackText = data.title,
                imageUrl = data.coverUrl,
                loader = loader,
                targetWidthPx = COVER_TARGET_WIDTH_PX,
                modifier = Modifier.fillMaxSize(),
            )

            /*
             * Метка типа — квадратный чип 24dp на затемнённой подложке
             * (`.hm3-card-type`). В макете внутри иконка из общего набора;
             * набора в модуле нет, а тащить его сюда ради одного глифа
             * дороже, чем поставить букву типа — она читается так же и не
             * добавляет модулю зависимости. Иконку подставлю одной строкой,
             * когда набор появится в `core:design`.
             */
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(Aurora.RadiusS))
                    .background(Aurora.Surface.copy(alpha = 0.65f))
                    .border(1.dp, Aurora.BrdEm, RoundedCornerShape(Aurora.RadiusS)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    CardText.kindShort(data.kind),
                    color = colorFor(data.kind),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            CardText.badgeLabel(data.badgeCount)?.let { badge ->
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(Aurora.RadiusS))
                        .background(Aurora.Acc)
                        // Размер по содержимому, а не жёсткий квадрат: «99+»
                        // в 22dp прижимается к краю — ту же ошибку уже
                        // ловили на бейдже календаря.
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Прогресс поверх обложки — только в рельсе, и с отступами от
            // краёв (`left/right/bottom: 8px`), а не в край: полоска впритык
            // к скруглению обложки выглядит артефактом отрисовки.
            if (style == TitleCardStyle.Rail && CardText.showProgress(data.progress)) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(CardText.clampProgress(data.progress))
                            .height(3.dp)
                            .background(Brush.horizontalGradient(listOf(Aurora.Acc2, Aurora.Acc))),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            data.title,
            color = Aurora.OnSurface,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )

        when (style) {
            TitleCardStyle.Rail -> {
                // В рельсе под названием только рейтинг, отдельной строкой.
                data.rating?.let {
                    Spacer(Modifier.height(3.dp))
                    Text("★ $it", color = Aurora.OnSurfaceVariant, fontSize = 10.sp)
                }
            }
            TitleCardStyle.Grid -> {
                data.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        it,
                        color = Aurora.OnSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (CardText.showProgress(data.progress)) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Aurora.SurfaceVariant),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(CardText.clampProgress(data.progress))
                                .height(3.dp)
                                .background(Aurora.Primary),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Широкая карточка для списка: обложка слева, текст справа. Нужна там, где
 * вертикальная сетка неуместна — в библиотеке списком и в результатах поиска.
 * Прямого аналога в макете нет; собрана из тех же токенов и подписей.
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
                .aspectRatio(COVER_RATIO)
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
                        .background(Aurora.SurfaceVariant),
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
                    .clip(RoundedCornerShape(Aurora.RadiusS))
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

/**
 * Обложка 118×168 в макете — это не ровно 2:3 (было бы 177), а чуть выше по
 * пропорции. Задаю тем же отношением, что в макете, а не «красивым» 2:3:
 * в ряду разница в 9dp видна как разъезжающиеся низы карточек.
 */
private const val COVER_RATIO = 118f / 168f

private const val COVER_TARGET_WIDTH_PX = 360
private const val ROW_COVER_TARGET_WIDTH_PX = 180
