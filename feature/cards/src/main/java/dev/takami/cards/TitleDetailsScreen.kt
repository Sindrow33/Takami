package dev.takami.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora

/**
 * Состояние источника для тайтла — секция `.t-srcbar` макета.
 *
 * Отдельная модель, а не строка: макет различает три состояния точкой
 * (норма, предупреждение, сломан), и от состояния зависит, показывать ли
 * баннер про мигрирование.
 */
data class TitleSourceStatus(
    val name: String,
    /** Что с источником: «активен · проверен минуту назад», «парсер сломан». */
    val detail: String,
    val level: Level = Level.Ok,
    /** Ссылка на страницу тайтла у источника; `null` — строки-ссылки нет. */
    val url: String? = null,
) {
    enum class Level { Ok, Warn, Broken }
}

/** Строка главы или эпизода — список `.chaps` макета. */
data class ChapterRowData(
    val id: String,
    val title: String,
    /**
     * Дата выхода. `null` — у локального файла её нет, и в этом случае
     * строка просто без подписи: выдуманная дата хуже отсутствующей.
     */
    val date: String? = null,
    val read: Boolean = false,
    /** Глава не открывается: у источника её нет или файл битый. */
    val broken: Boolean = false,
    /**
     * Скачана. Для локальной папки всегда true (файл уже на диске),
     * для сетевого источника — по кешу.
     */
    val downloaded: Boolean = false,
)

/**
 * Экран тайтла по макету `kit/Title.jsx`.
 *
 * Компоновка героя — из макета: размытая подложка цветом обложки, поверх неё
 * обложка 2:3 шириной 108dp слева и мета справа. Раньше здесь была
 * широкая картинка 16:10 с названием поверх — я делал её по догадке, пока
 * макета не было; настоящий макет так не устроен.
 *
 * Блоки показываются только когда данные есть. Пустой «Описание» под
 * названием читается как не загрузившийся текст, а у разобранных тайтлов
 * описания часто нет вовсе.
 */
@Composable
fun TitleDetailsScreen(
    data: TitleCardData,
    modifier: Modifier = Modifier,
    description: String? = null,
    genres: List<String> = emptyList(),
    author: String? = null,
    year: String? = null,
    characters: List<CharacterCardData> = emptyList(),
    chapters: List<ChapterRowData> = emptyList(),
    source: TitleSourceStatus? = null,
    loader: ImageLoader? = null,
    inLibrary: Boolean = false,
    onToggleLibrary: (() -> Unit)? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    onChangeSource: (() -> Unit)? = null,
    onOpenChapter: ((ChapterRowData) -> Unit)? = null,
    onOpenCharacter: ((CharacterCardData) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(data, genres, author, year, loader, onBack)

        ActionRow(
            inLibrary = inLibrary,
            onToggleLibrary = onToggleLibrary,
            primaryActionLabel = primaryActionLabel,
            onPrimaryAction = onPrimaryAction,
        )

        source?.let {
            SourceBar(it, onChangeSource)
            if (it.level == TitleSourceStatus.Level.Broken) BrokenBanner()
        }

        description?.takeIf { it.isNotBlank() }?.let { Description(it) }

        Spacer(Modifier.height(4.dp))
        CharacterRail(characters = characters, loader = loader, onOpen = onOpenCharacter)

        if (chapters.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Chapters(data.kind, chapters, onOpenChapter)
        }
        Spacer(Modifier.height(120.dp)) // место под нижнюю панель
    }
}

@Composable
private fun Hero(
    data: TitleCardData,
    genres: List<String>,
    author: String?,
    year: String?,
    loader: ImageLoader?,
    onBack: (() -> Unit)?,
) {
    Box {
        /*
         * Подложка: та же обложка, размытая и приглушённая. В макете это
         * `filter: blur(28px) saturate(1.2); opacity: .55` плюс градиент в
         * цвет фона к низу — иначе подложка обрывается видимым краем.
         */
        Box(Modifier.matchHero()) {
            CardImage(
                id = data.id,
                fallbackText = data.title,
                imageUrl = data.coverUrl,
                loader = loader,
                targetWidthPx = HERO_TARGET_WIDTH_PX,
                modifier = Modifier.fillMaxSize().blur(28.dp),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Aurora.Surface.copy(alpha = 0.45f))
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.6f to Color.Transparent,
                            1f to Aurora.Surface,
                        )
                    )
            )
        }

        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                Modifier
                    .width(108.dp)
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
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    data.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                // Вторая строка меты в макете — «Автор · Выходит». Ни автора,
                // ни статуса выхода источники нам не отдают, поэтому строка
                // рисуется только если данные пришли, а не с прочерками.
                author?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Aurora.OnSurfaceVariant, fontSize = 12.sp)
                }
                Text(
                    listOfNotNull(
                        CardText.kindLabel(data.kind),
                        data.subtitle?.takeIf { it.isNotBlank() },
                        year?.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    color = Aurora.OnSurfaceVariant,
                    fontSize = 12.sp,
                )
                data.rating?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "★ $it",
                        color = Aurora.Warn,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (genres.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Genres(genres)
                }
            }
        }

        if (onBack != null) {
            Box(
                Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(Aurora.RadiusS))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("‹", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Жанры пилюлями. Переносятся на вторую строку: `flex-wrap` в макете. */
@Composable
private fun Genres(genres: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        genres.chunked(GENRES_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { genre ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(Aurora.RadiusFull))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(Aurora.RadiusFull),
                            )
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(genre, color = Aurora.OnSurface, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Ряд действий: «В библиотеку» и «Продолжить». Обе кнопки-пилюли, главная
 * шире (в макете `flex: 1.4`) и с градиентной заливкой.
 */
@Composable
private fun ActionRow(
    inLibrary: Boolean,
    onToggleLibrary: (() -> Unit)?,
    primaryActionLabel: String?,
    onPrimaryAction: (() -> Unit)?,
) {
    if (onToggleLibrary == null && (primaryActionLabel == null || onPrimaryAction == null)) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onToggleLibrary != null) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .background(Aurora.SurfaceContainer)
                    .border(
                        1.dp,
                        if (inLibrary) Aurora.Acc.copy(alpha = 0.3f) else Aurora.Brd,
                        RoundedCornerShape(Aurora.RadiusFull),
                    )
                    .clickable { onToggleLibrary() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (inLibrary) "В библиотеке" else "В библиотеку",
                    color = if (inLibrary) Aurora.Acc else Aurora.OnSurface,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
        if (primaryActionLabel != null && onPrimaryAction != null) {
            Box(
                Modifier
                    .weight(1.4f)
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .background(Aurora.AccentGradient)
                    .clickable { onPrimaryAction() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    primaryActionLabel,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Состояние источника: точка-семафор, название, подпись, «Сменить». */
@Composable
private fun SourceBar(status: TitleSourceStatus, onChangeSource: (() -> Unit)?) {
    Spacer(Modifier.height(16.dp))
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Aurora.RadiusM))
            .background(Aurora.SurfaceContainer)
            .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .background(
                        when (status.level) {
                            TitleSourceStatus.Level.Ok -> Aurora.Ok
                            TitleSourceStatus.Level.Warn -> Aurora.Warn
                            TitleSourceStatus.Level.Broken -> Aurora.Error
                        }
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    status.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    status.detail,
                    color = Aurora.OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onChangeSource != null) {
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(Aurora.SurfaceVariant)
                        .clickable { onChangeSource() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Сменить", color = Aurora.OnSurface, fontSize = 11.sp)
                }
            }
        }
        // Адрес у источника — отдельной строкой, без схемы: «https://» в
        // узкой строке съедает место, а домен и есть то, что читают.
        status.url?.takeIf { it.isNotBlank() }?.let { url ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("↗", color = Aurora.Acc, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    url.removePrefix("https://").removePrefix("http://"),
                    color = Aurora.OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("›", color = Aurora.OnSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

/** Баннер про сломанный источник: что случилось и что можно сделать. */
@Composable
private fun BrokenBanner() {
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Aurora.RadiusM))
            .background(Aurora.Error.copy(alpha = 0.10f))
            .border(
                1.dp,
                Aurora.Error.copy(alpha = 0.35f),
                RoundedCornerShape(Aurora.RadiusM),
            )
            .padding(12.dp),
    ) {
        Text("!", color = BANNER_TEXT, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Text(
            "Часть глав не найдена у этого источника. Можно сменить источник — " +
                "прогресс сохранится.",
            color = BANNER_TEXT,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

/**
 * Описание с ограничением в три строки и раскрытием по «Читать полностью».
 * Без ограничения длинное описание отодвигает персонажей и главы за экран.
 */
@Composable
private fun Description(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    Spacer(Modifier.height(18.dp))
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            text,
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_CLAMP_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        if (!expanded) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Читать полностью ›",
                color = Aurora.Acc,
                fontSize = 12.sp,
                modifier = Modifier.clickable { expanded = true },
            )
        }
    }
}

/** Список глав или эпизодов. Прочитанные приглушены, битые помечены. */
@Composable
private fun Chapters(
    kind: ContentKind,
    chapters: List<ChapterRowData>,
    onOpenChapter: ((ChapterRowData) -> Unit)?,
) {
    SectionHead(
        title = if (kind == ContentKind.Anime) "Эпизоды" else "Главы",
        action = "Все ${chapters.size}",
    )
    Spacer(Modifier.height(4.dp))
    Column(Modifier.padding(horizontal = 16.dp)) {
        chapters.forEach { chapter ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (onOpenChapter != null && !chapter.broken) {
                            Modifier.clickable { onOpenChapter(chapter) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        chapter.title,
                        // Прочитанное приглушено, битое — цветом ошибки:
                        // иначе в длинном списке не видно, где остановился.
                        color = when {
                            chapter.broken -> BANNER_TEXT
                            chapter.read -> Aurora.OnSurfaceVariant
                            else -> Color.White
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val note = if (chapter.broken) "Не загружается" else chapter.date
                    note?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            it,
                            color = if (chapter.broken) BANNER_TEXT else Aurora.OnSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (chapter.downloaded) "✓" else "↓",
                    color = if (chapter.downloaded) Aurora.Ok else Aurora.OnSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Aurora.OutlineVar)
            )
        }
    }
}

/** Высота подложки героя: обложка 108dp в пропорции 2:3 плюс отступы. */
private fun Modifier.matchHero(): Modifier = this
    .fillMaxWidth()
    .height(HERO_HEIGHT)

private val HERO_HEIGHT = 194.dp
private val BANNER_TEXT = Color(0xFFFF9E9E)
private const val DESCRIPTION_CLAMP_LINES = 3
private const val GENRES_PER_ROW = 3
private const val HERO_TARGET_WIDTH_PX = 480
private const val COVER_TARGET_WIDTH_PX = 360
