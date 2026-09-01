package dev.takami.app.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.home.ContentType
import dev.takami.app.ui.theme.Aurora
import java.util.Calendar

/**
 * Календарь релизов: полоса дней с цветными точками по типу контента и
 * список релизов выбранного дня.
 *
 * Экран пока на локальной выборке — расписание приедет из автопарсера
 * вместе с источниками. Форма данных ([DayRelease]) подобрана под то,
 * что парсер реально сможет отдать: дата, тип, название и номер выпуска;
 * ничего, чего нет на странице тайтла.
 */
@Composable
fun CalendarScreen(schedule: ReleaseSchedule = ReleaseSchedule.demo()) {
    val today = remember { startOfToday() }
    var selectedDay by remember { mutableStateOf(today) }
    val days = remember(today) { schedule.daysAround(today) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        Text(
            "Календарь",
            color = Aurora.OnSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
        )
        Text(
            "Что выходит на этой неделе",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 14.dp),
        )

        DayStrip(
            days = days,
            today = today,
            selected = selectedDay,
            schedule = schedule,
            onSelect = { selectedDay = it },
        )

        Spacer(Modifier.height(20.dp))
        DayReleases(schedule.releasesOn(selectedDay), isToday = selectedDay == today)
    }
}

@Composable
private fun DayStrip(
    days: List<Long>,
    today: Long,
    selected: Long,
    schedule: ReleaseSchedule,
    onSelect: (Long) -> Unit,
) {
    val state = rememberLazyListState()
    // Полоса открывается на сегодняшнем дне, а не в начале недели:
    // прошедшие дни видны прокруткой назад, но не занимают экран.
    LaunchedEffect(days) {
        val index = days.indexOf(today)
        if (index > 0) state.scrollToItem(index)
    }

    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days) { day ->
            DayCell(
                day = day,
                isToday = day == today,
                isSelected = day == selected,
                types = schedule.typesOn(day),
                onClick = { onSelect(day) },
            )
        }
    }
}

@Composable
private fun DayCell(
    day: Long,
    isToday: Boolean,
    isSelected: Boolean,
    types: List<ContentType>,
    onClick: () -> Unit,
) {
    val cal = remember(day) { Calendar.getInstance().apply { timeInMillis = day } }
    Column(
        Modifier
            .width(54.dp)
            .clip(RoundedCornerShape(Aurora.RadiusM))
            .background(if (isSelected) Aurora.SurfaceContainer else Aurora.Sub)
            .border(
                1.dp,
                if (isSelected) Aurora.Acc.copy(alpha = .55f) else Color.Transparent,
                RoundedCornerShape(Aurora.RadiusM),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK) - 1],
            color = if (isToday) Aurora.Acc2 else Aurora.OnSurfaceVariant,
            fontSize = 10.sp,
        )
        Text(
            "${cal.get(Calendar.DAY_OF_MONTH)}",
            color = Aurora.OnSurface,
            fontSize = 16.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
        )
        // Точки по типам контента; пустой день оставляет ровно столько же
        // места, чтобы полоса не дёргалась по высоте при прокрутке.
        Row(
            Modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            types.forEach { type ->
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(Aurora.RadiusFull)).background(type.color))
            }
        }
    }
}

@Composable
private fun DayReleases(releases: List<DayRelease>, isToday: Boolean) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (isToday) "Сегодня" else "Релизы дня",
            color = Aurora.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (releases.isEmpty()) {
            Text(
                "В этот день релизов нет",
                color = Aurora.OnSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            return@Column
        }
        releases.forEach { release ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Aurora.RadiusM))
                    .background(Aurora.SurfaceContainer)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(release.type.color),
                )
                Column(Modifier.weight(1f)) {
                    Text(release.title, color = Aurora.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "${release.type.label} · ${release.issue}",
                        color = Aurora.OnSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(release.time, color = Aurora.OnSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

private val WEEKDAYS = listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб")

internal fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
