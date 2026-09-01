package dev.takami.app.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anime.player.host.AnimeScreen
import dev.takami.app.ui.theme.Aurora

/**
 * Вкладка «Библиотека»: манга и аниме одним списком-переключателем.
 *
 * Почему не отдельный таб в нижней панели: панель по дизайну пять
 * позиций, центральная — «Свайпы». Шестая позиция ломает раскладку, а
 * аниме и манга — это одна и та же локальная библиотека, просто разного
 * типа контента. Переключатель здесь дешевле и честнее.
 */
private enum class Kind { Manga, Anime }

@Composable
fun LibraryTabs() {
    var kind by remember { mutableStateOf(Kind.Manga) }
    val context = LocalContext.current
    val root = remember { LibraryRoot(context) }

    Column(Modifier.fillMaxSize().background(Aurora.Surface)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(Aurora.RadiusFull))
                .background(Color(0x0FFFFFFF))
                .padding(4.dp),
        ) {
            Segment("Манга", kind == Kind.Manga, Modifier.weight(1f)) { kind = Kind.Manga }
            Segment("Аниме", kind == Kind.Anime, Modifier.weight(1f)) { kind = Kind.Anime }
        }

        when (kind) {
            Kind.Manga -> LibraryScreen()
            /*
             * Плеер читает выбранную папку через content:// напрямую —
             * серию копировать нельзя, это сотни мегабайт на просмотр.
             * Внутренний каталог остаётся запасным путём, когда папка
             * не выбрана.
             */
            Kind.Anime -> AnimeScreen(
                contentTree = root.selectedTree(),
                contentRoot = root.internalDir(),
            )
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(Aurora.RadiusFull))
            .then(if (selected) Modifier.background(Aurora.AccentGradient) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.White else Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
