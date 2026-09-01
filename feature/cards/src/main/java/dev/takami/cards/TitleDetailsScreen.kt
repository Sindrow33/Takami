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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

/**
 * Экран тайтла: обложка-подложка, сведения, описание, персонажи.
 *
 * Описание и персонажи показываются только когда есть: пустой блок
 * «Описание» под названием выглядит как не загрузившийся текст, а у
 * разобранных тайтлов описания часто нет вовсе.
 */
@Composable
fun TitleDetailsScreen(
    data: TitleCardData,
    modifier: Modifier = Modifier,
    description: String? = null,
    characters: List<CharacterCardData> = emptyList(),
    loader: ImageLoader? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
            CardImage(
                id = data.id,
                fallbackText = data.title,
                imageUrl = data.coverUrl,
                loader = loader,
                targetWidthPx = HERO_TARGET_WIDTH_PX,
                modifier = Modifier.fillMaxSize(),
            )
            // Затемнение к низу: название ложится на обложку, и без
            // градиента белый текст на светлой картинке не читается.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.35f),
                            0.5f to Color.Transparent,
                            1f to Aurora.Surface,
                        )
                    )
            )
            if (onBack != null) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(Aurora.RadiusS))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) { Text("‹", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    CardText.kindLabel(data.kind),
                    color = colorFor(data.kind),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    data.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            data.rating?.let {
                Text("★ $it", color = Aurora.Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
            }
            data.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Aurora.OnSurfaceVariant, fontSize = 13.sp)
            }
        }

        if (primaryActionLabel != null && onPrimaryAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Aurora.RadiusM))
                    .background(Aurora.AccentGradient)
                    .clickable { onPrimaryAction() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    primaryActionLabel,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        description?.takeIf { it.isNotBlank() }?.let { text ->
            Spacer(Modifier.height(18.dp))
            Text(
                "Описание",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text,
                color = Aurora.OnSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Spacer(Modifier.height(22.dp))
        CharacterRail(characters = characters, loader = loader)
        Spacer(Modifier.height(120.dp)) // место под нижнюю панель
    }
}

private const val HERO_TARGET_WIDTH_PX = 1080
