package dev.takami.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangareader.feature.reader.ReaderSourceRegistry
import core.engine.ParserStats
import dev.takami.app.parser.AutoParseScreen
import dev.takami.app.parser.ParserState
import dev.takami.app.ui.theme.Aurora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Настройки: источники, прокси, автопарсер и поддержка разработки.
 *
 * Сводка автопарсера — реальная, из `ParserStatsProvider`. Пока
 * источников нет, показывается честная пустая сводка, а не выдуманные
 * проценты: тот же принцип, что и у индикатора на главной.
 */
@Composable
fun SettingsScreen(prefs: AppSettingsStore = AppSettingsStore(LocalContext.current)) {
    var probing by remember { mutableStateOf(false) }
    if (probing) {
        AutoParseScreen(onClose = { probing = false })
        return
    }
    SettingsBody(prefs = prefs, onProbe = { probing = true })
}

@Composable
private fun SettingsBody(prefs: AppSettingsStore, onProbe: () -> Unit) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(ParserStats.EMPTY) }
    var proxyEnabled by remember { mutableStateOf(prefs.proxyEnabled) }
    var autoHealEnabled by remember { mutableStateOf(prefs.autoHealEnabled) }
    var wifiOnlyDownloads by remember { mutableStateOf(prefs.wifiOnlyDownloads) }
    var cacheBytes by remember { mutableStateOf(0L) }
    var cacheRefresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val libraryRoot = remember { dev.takami.app.library.LibraryRoot(context) }
    var libraryPath by remember { mutableStateOf(libraryRoot.displayPath()) }
    val folderPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            libraryRoot.select(uri)
            libraryPath = libraryRoot.displayPath()
        }
    }

    LaunchedEffect(Unit) {
        stats = withContext(Dispatchers.IO) { ParserState(context).stats() }
    }
    LaunchedEffect(cacheRefresh) {
        cacheBytes = withContext(Dispatchers.IO) {
            ReaderSourceRegistry.diskCache?.sizeBytes() ?: 0L
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Настройки", color = Aurora.OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Section("Автопарсер") {
            ParserSummary(stats)
            Toggle(
                title = "Самопочинка",
                subtitle = "Восстанавливать конфиг источника при изменении разметки",
                checked = autoHealEnabled,
                onChange = { autoHealEnabled = it; prefs.autoHealEnabled = it },
            )
        }

        Section("Источники") {
            InfoRow(
                title = "Подключено источников",
                value = "${stats.sourceCount}",
            )
            InfoRow(
                title = "Локальная библиотека",
                value = "папки и CBZ",
            )
            Hint("Онлайн-источники появятся здесь после подключения парсера.")
            Spacer(Modifier.height(10.dp))
            Text(
                "Разобрать сайт по ссылке",
                color = Aurora.Acc2,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .clickable(onClick = onProbe)
                    .padding(vertical = 4.dp),
            )
            Hint("Даёт автопарсеру адрес страницы и показывает, что он с неё снял.")
        }

        Section("Хранилище") {
            /*
             * Папка контента — первым пунктом. По умолчанию корнем
             * остаётся внутренний каталог приложения, куда пользователь
             * с телефона положить ничего не может, поэтому смена папки
             * должна быть на виду, а не только на пустом экране
             * библиотеки.
             */
            InfoRow(title = "Папка с контентом", value = libraryPath)
            Text(
                "Выбрать папку",
                color = Aurora.Acc2,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(Aurora.RadiusFull))
                    .clickable { folderPicker.launch(null) }
                    .padding(vertical = 4.dp),
            )
            Hint(
                "Главы читаются отсюда: Название/Глава/0001.jpg или Название/Глава.cbz. " +
                    "Сюда же будут сохраняться скачанные главы.",
            )
            InfoRow(title = "Кеш страниц", value = formatSize(cacheBytes))
            Hint(
                "Скачанные страницы хранятся, чтобы перечитывание не тратило трафик. " +
                    "Предел — ${formatSize(prefs.pageCacheLimitBytes)}; система может очистить кеш сама при нехватке места.",
            )
            if (cacheBytes > 0) {
                Text(
                    "Очистить кеш",
                    color = Aurora.Acc2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .clickable {
                            scope.launch {
                                withContext(Dispatchers.IO) { ReaderSourceRegistry.diskCache?.clear() }
                                cacheRefresh++
                            }
                        }
                        .padding(vertical = 4.dp),
                )
            }
        }

        Section("Сеть") {
            Toggle(
                title = "Прокси",
                subtitle = "Для источников, недоступных напрямую",
                checked = proxyEnabled,
                onChange = { proxyEnabled = it; prefs.proxyEnabled = it },
            )
            Toggle(
                title = "Загрузки только по Wi-Fi",
                subtitle = "Не тратить мобильный трафик на главы",
                checked = wifiOnlyDownloads,
                onChange = { wifiOnlyDownloads = it; prefs.wifiOnlyDownloads = it },
            )
        }

        Section("Поддержать разработку") {
            Hint(
                "Takami делается без рекламы и подписок. Если приложение оказалось " +
                    "полезным — расскажите о нём тем, кто читает то же, что и вы.",
            )
        }
    }
}

@Composable
private fun ParserSummary(stats: ParserStats) {
    val hasData = stats.sourceCount > 0
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCell("Обучаемость", if (hasData) "${stats.learningPercent}%" else "—", Aurora.Acc2, Modifier.weight(1f))
        StatCell("Точность", if (hasData) "${stats.accuracyPercent}%" else "—", Aurora.Ok, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCell("Самопочинок", "${stats.selfHealCount}", Aurora.Ok, Modifier.weight(1f))
        StatCell(
            "Аномалий",
            "${stats.anomalyCount}",
            if (stats.anomalyCount > 0) Aurora.Warn else Aurora.Ok,
            Modifier.weight(1f),
        )
    }
    if (!hasData) {
        Spacer(Modifier.height(8.dp))
        Hint("Движок начнёт учиться после первых разборов.")
    }
}

@Composable
private fun StatCell(label: String, value: String, tone: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Aurora.RadiusM))
            .background(Aurora.Sub)
            .padding(14.dp),
    ) {
        Text(value, color = tone, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Aurora.OnSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Aurora.OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Aurora.RadiusM))
                .background(Aurora.SurfaceContainer)
                .border(1.dp, Aurora.Brd, RoundedCornerShape(Aurora.RadiusM))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Aurora.OnSurface, fontSize = 14.sp)
            Text(subtitle, color = Aurora.OnSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Aurora.OnPrimary,
                checkedTrackColor = Aurora.Acc,
                uncheckedTrackColor = Aurora.Sub,
                uncheckedBorderColor = Aurora.Brd,
            ),
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Aurora.OnSurface, fontSize = 14.sp)
        Text(value, color = Aurora.OnSurfaceVariant, fontSize = 13.sp)
    }
}

/** Человекочитаемый размер: КБ/МБ/ГБ без лишней точности. */
internal fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "пусто"
    bytes < 1024L * 1024 -> "${bytes / 1024} КБ"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} МБ"
    else -> String.format("%.1f ГБ", bytes / (1024.0 * 1024 * 1024))
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Aurora.OnSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
}
