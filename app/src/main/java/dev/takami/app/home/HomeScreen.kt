package dev.takami.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import core.engine.ParserStats
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import dev.takami.app.ui.components.Icon
import dev.takami.app.ui.components.Pill
import dev.takami.app.ui.components.Tab
import dev.takami.app.ui.components.TakamiIcon
import dev.takami.app.ui.theme.Aurora
import java.util.Calendar

@Composable
fun HomeScreen(
    parserStats: ParserStats = ParserStats.EMPTY,
    onOpenTitle: (Int) -> Unit = {},
    onParseSite: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
    ) {
        TopBar(parserStats)
        Spacer(Modifier.height(16.dp))
        HeroContinue(MockDb.hero, onOpenTitle)
        Spacer(Modifier.height(20.dp))
        QuickActions(onParseSite = onParseSite)
        Spacer(Modifier.height(24.dp))
        NewsRail()
        Rail("Продолжить", MockDb.continueReading, onOpenTitle)
        Rail("Манга", MockDb.byType(ContentType.Manga), onOpenTitle)
        Rail("Аниме", MockDb.byType(ContentType.Anime), onOpenTitle)
        Rail("Ранобэ", MockDb.byType(ContentType.Novel), onOpenTitle)
    }
}

/** Приветствие по времени суток — интервалы из хендоффа (0-5 / 5-12 / 12-18 / 18-24). */
internal fun greetingFor(hour: Int): String = when {
    hour < 5 -> "Доброй ночи"
    hour < 12 -> "Доброе утро"
    hour < 18 -> "Добрый день"
    else -> "Добрый вечер"
}

@Composable
private fun TopBar(parserStats: ParserStats) {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val weekdays = listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб")
    val dateLine = "${weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]}, ${cal.get(Calendar.DAY_OF_MONTH)}".uppercase()

    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(dateLine, color = Aurora.OnSurfaceVariant, fontSize = 11.sp, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(2.dp))
            /*
             * Одна строка с автоуменьшением. Раньше приветствие с именем
             * жило в Row из двух Text: они переносились по отдельности и
             * ломались на три строки, наезжая на индикатор автопарсера.
             */
            Text(
                text = "${greetingFor(hour)}, Читатель",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AiIndicator(parserStats)
            Icon(TakamiIcon.Search, Modifier.size(22.dp), Color.White)
            Icon(TakamiIcon.Settings, Modifier.size(22.dp), Color.White)
        }
    }
}

@Composable
private fun HeroContinue(item: TitleItem, onOpen: (Int) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(Aurora.RadiusL))
            .background(Brush.linearGradient(listOf(item.coverA, item.coverB)))
            .clickable { onOpen(item.id) }
    ) {
        Pill(
            "Продолжить · ${item.type.label}",
            Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(Aurora.RadiusM))
                .background(Color(0x40FFFFFF))
                .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM))
                .padding(12.dp)
        ) {
            Text(item.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(item.subtitle, color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(Modifier.fillMaxWidth(item.progress).height(2.dp).background(Aurora.Primary))
                }
                Text("${(item.progress * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .background(Aurora.AccentGradient)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (item.type == ContentType.Anime) TakamiIcon.Play else TakamiIcon.Book,
                    Modifier.size(15.dp), Color.White,
                )
                Text(
                    if (item.type == ContentType.Anime) "Смотреть" else "Читать",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun QuickActions(onParseSite: () -> Unit = {}) {
    /*
     * Четвёртая карточка — «Добавить сайт». Кнопка разбора жила только в
     * глубине настроек, и её там не нашли: действие, которым источники
     * вообще появляются в приложении, обязано быть на главной.
     */
    data class Action(val icon: TakamiIcon, val label: String, val sub: String, val onClick: () -> Unit)
    val actions = listOf(
        Action(TakamiIcon.Bell, "Обновления", "3") {},
        Action(TakamiIcon.Calendar, "Календарь", "сегодня") {},
        Action(TakamiIcon.Search, "Поиск", "по кадру") {},
        Action(TakamiIcon.Brain, "Добавить сайт", "автопарсер", onParseSite),
    )
    // В макете это карточки-коробки с обводкой, а не голые глифы:
    // так они читаются как кнопки и попасть по ним проще.
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEach { action ->
            val (icon, label, sub) = Triple(action.icon, action.label, action.sub)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Aurora.RadiusM))
                    .background(Aurora.SurfaceContainer)
                    .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM))
                    .clickable(onClick = action.onClick)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, Modifier.size(22.dp), Aurora.Acc2)
                Spacer(Modifier.height(7.dp))
                Text(
                    label, color = Color.White, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    sub, color = Aurora.OnSurfaceVariant, fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Rail(title: String, items: List<TitleItem>, onOpen: (Int) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        SectionHeader(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { TitleCard(it, onOpen) }
        }
    }
}

@Composable
private fun TitleCard(item: TitleItem, onOpen: (Int) -> Unit) {
    Column(Modifier.width(108.dp).clickable { onOpen(item.id) }) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(144.dp)
                .clip(RoundedCornerShape(Aurora.RadiusM))
                .background(Brush.linearGradient(listOf(item.coverA, item.coverB)))
        ) {
            Icon(
                when (item.type) {
                    ContentType.Anime -> TakamiIcon.Play
                    else -> TakamiIcon.Book
                },
                Modifier.align(Alignment.TopStart).padding(6.dp).size(14.dp),
                Color.White.copy(alpha = .85f),
            )
            if (item.newChapters > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(Aurora.Acc)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${item.newChapters}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (item.progress > 0f) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).background(Color(0x33FFFFFF))) {
                    Box(Modifier.fillMaxWidth(item.progress).height(2.dp).background(item.type.color))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.name, color = Color.White, fontSize = 12.sp, maxLines = 2, lineHeight = 16.sp)
        Text("★ ${item.rating}", color = Aurora.OnSurfaceVariant, fontSize = 10.sp)
    }
}
