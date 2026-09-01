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
    onOpenTitle: (String) -> Unit = {},
    onOpenLibrary: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var feed by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<LibraryFeed.Feed?>(null)
    }

    /*
     * Наполнение читается с диска при каждом показе экрана: папка могла
     * смениться, главы могли дочитаться. Обход идёт через content://,
     * поэтому вне главного потока.
     */
    androidx.compose.runtime.LaunchedEffect(Unit) {
        feed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            LibraryFeed.load(context)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
    ) {
        TopBar(parserStats)
        Spacer(Modifier.height(16.dp))

        val loaded = feed
        when {
            loaded == null -> Unit // первый кадр: ничего не выдумываем

            loaded.isEmpty -> {
                QuickActions()
                Spacer(Modifier.height(24.dp))
                /*
                 * Честное пустое состояние вместо выдуманных карточек.
                 * Красивая главная на моках дважды была принята за
                 * работающее приложение — пустой экран, который
                 * объясняет, чего не хватает, полезнее.
                 */
                EmptyHome(
                    folderChosen = loaded.folderChosen,
                    hasSources = loaded.hasSources,
                    onOpenLibrary = onOpenLibrary,
                )
            }

            else -> {
                loaded.hero?.let { hero ->
                    HeroContinue(hero, onOpenTitle)
                    Spacer(Modifier.height(20.dp))
                }
                QuickActions()
                Spacer(Modifier.height(24.dp))
                Rail("Продолжить", loaded.continueReading, onOpenTitle)
                Rail("Манга", loaded.manga, onOpenTitle)
                Rail("Ранобэ", loaded.novels, onOpenTitle)
                Rail("Аниме", loaded.anime, onOpenTitle)
            }
        }
    }
}

/**
 * Что показать, когда показывать нечего.
 *
 * Текст зависит от того, чего именно не хватает: совет «выберите папку»
 * человеку, который её уже выбрал, выглядит как неисправность.
 */
@Composable
private fun EmptyHome(
    folderChosen: Boolean,
    hasSources: Boolean,
    onOpenLibrary: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Пока пусто",
            color = Aurora.OnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                !folderChosen ->
                    "Выберите папку с мангой или ранобэ — главы появятся здесь. " +
                        "Онлайн-тайтлы добавятся, когда разберёте сайт в настройках."
                !hasSources ->
                    "В выбранной папке глав не нашлось. Ожидаемая раскладка: " +
                        "Название/Глава/0001.jpg, Название/Глава.cbz или Название/Глава.txt."
                else ->
                    "Источник разобран, но тайтлов пока нет. Откройте каталог источника, " +
                        "чтобы добавить первый."
            },
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            if (folderChosen) "Открыть библиотеку" else "Выбрать папку",
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(Aurora.RadiusFull))
                .background(Aurora.Acc2)
                .clickable(onClick = onOpenLibrary)
                .padding(horizontal = 22.dp, vertical = 14.dp),
        )
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
private fun HeroContinue(item: LibraryFeed.TitleCardData, onOpen: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(Aurora.RadiusL))
            // Обложек у локальных файлов нет. Цвет выводится из имени,
            // чтобы карточки различались между собой и не прыгали от
            // запуска к запуску — это плейсхолдер, а не выдуманная
            // картинка.
            .background(Brush.linearGradient(coverColors(item.title, item.kind)))
            .clickable { onOpen(item.id) }
    ) {
        Pill(
            "Продолжить · ${item.kind.label}",
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
            Text(item.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            item.subtitle?.let {
                Text(it, color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
            }
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
                    if (item.kind == ContentType.Anime) TakamiIcon.Play else TakamiIcon.Book,
                    Modifier.size(15.dp), Color.White,
                )
                Text(
                    if (item.kind == ContentType.Anime) "Смотреть" else "Читать",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun QuickActions() {
    /*
     * Разбора сайта здесь больше нет. Отдельная карточка «Добавить
     * сайт» вела на пустой экран, который никто не связывал с
     * автопарсером; теперь поле адреса и кнопка живут прямо в сводке
     * движка — там, где пользователь на него и смотрит.
     */
    data class Action(val icon: TakamiIcon, val label: String, val sub: String, val onClick: () -> Unit)
    val actions = listOf(
        Action(TakamiIcon.Bell, "Обновления", "3") {},
        Action(TakamiIcon.Calendar, "Календарь", "сегодня") {},
        Action(TakamiIcon.Search, "Поиск", "по кадру") {},
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
private fun Rail(
    title: String,
    items: List<LibraryFeed.TitleCardData>,
    onOpen: (String) -> Unit,
) {
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
private fun TitleCard(item: LibraryFeed.TitleCardData, onOpen: (String) -> Unit) {
    Column(Modifier.width(108.dp).clickable { onOpen(item.id) }) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(144.dp)
                .clip(RoundedCornerShape(Aurora.RadiusM))
                .background(Brush.linearGradient(coverColors(item.title, item.kind)))
        ) {
            Icon(
                when (item.kind) {
                    ContentType.Anime -> TakamiIcon.Play
                    else -> TakamiIcon.Book
                },
                Modifier.align(Alignment.TopStart).padding(6.dp).size(14.dp),
                Color.White.copy(alpha = .85f),
            )
            if (item.badgeCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(Aurora.Acc)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${item.badgeCount}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (item.progress > 0f) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).background(Color(0x33FFFFFF))) {
                    Box(Modifier.fillMaxWidth(item.progress).height(2.dp).background(item.kind.color))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, color = Color.White, fontSize = 12.sp, maxLines = 2, lineHeight = 16.sp)
        // Рейтинга у локального файла нет, и «★ —» было бы такой же
        // выдумкой, как «★ 8.7»: подпись просто не рисуется.
        item.rating?.let {
            Text("★ $it", color = Aurora.OnSurfaceVariant, fontSize = 10.sp)
        } ?: item.subtitle?.let {
            Text(it, color = Aurora.OnSurfaceVariant, fontSize = 10.sp, maxLines = 1)
        }
    }
}

/**
 * Цвета плейсхолдера обложки.
 *
 * Выводятся из названия, а не берутся случайно: карточка должна
 * выглядеть одинаково при каждом запуске, иначе библиотека «мерцает»
 * разными цветами на одних и тех же тайтлах. Оттенок задаёт тип
 * контента — так манга, ранобэ и аниме различимы без подписи.
 */
private fun coverColors(title: String, kind: ContentType): List<Color> {
    val hash = title.fold(0) { acc, ch -> acc * 31 + ch.code }
    val palette = when (kind) {
        ContentType.Manga -> listOf(
            Color(0xFF4C1D95), Color(0xFF134E4A), Color(0xFF7C2D12), Color(0xFF1E3A8A),
        )
        ContentType.Novel -> listOf(
            Color(0xFF5B21B6), Color(0xFF831843), Color(0xFF3F3F46), Color(0xFF14532D),
        )
        ContentType.Anime -> listOf(
            Color(0xFF0C4A6E), Color(0xFF701A75), Color(0xFF854D0E), Color(0xFF1E293B),
        )
    }
    val top = palette[Math.floorMod(hash, palette.size)]
    return listOf(top, Color(0xFF141821))
}
