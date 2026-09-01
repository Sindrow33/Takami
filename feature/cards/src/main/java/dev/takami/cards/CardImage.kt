package dev.takami.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import dev.takami.app.ui.theme.Aurora

/**
 * Картинка карточки с честным плейсхолдером.
 *
 * Плейсхолдер — не серый прямоугольник, а инициалы на устойчивом по
 * идентификатору градиенте: обложки у разобранных тайтлов часто нет, и это
 * обычное состояние, а не сбой. Карточка обязана оставаться узнаваемой.
 */
@Composable
fun CardImage(
    id: String,
    fallbackText: String,
    imageUrl: String?,
    loader: ImageLoader?,
    targetWidthPx: Int,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(id, imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(id, imageUrl) {
        if (imageUrl != null && loader != null) {
            bitmap = loader.load(imageUrl, targetWidthPx)
        }
    }

    Box(modifier) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val palette = PLACEHOLDER_PALETTE
            val pair = palette[CardText.placeholderIndex(id, palette.size)]
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(pair.first, pair.second))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    CardText.initials(fallbackText),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Палитра плейсхолдеров из токенов оформления. Пары, а не одиночные цвета:
 * плоская заливка рядом с настоящими обложками читается как «картинка не
 * загрузилась», градиент — как оформление.
 */
private val PLACEHOLDER_PALETTE = listOf(
    Aurora.GradA to Aurora.GradB,
    Aurora.Acc to Aurora.AccDim,
    Aurora.Acc2 to Aurora.Acc,
    Aurora.AccBlue to Aurora.AccDim,
    Aurora.Acc3 to Aurora.AccBlue,
    Aurora.HeartPink to Aurora.Acc2,
)
