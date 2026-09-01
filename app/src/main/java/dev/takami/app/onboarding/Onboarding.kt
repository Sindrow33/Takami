package dev.takami.app.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.takami.app.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.takami.app.ui.components.*
import dev.takami.app.ui.theme.Aurora
import kotlinx.coroutines.delay

enum class OnbStep { Splash, Policy, Perms, Welcome }

/**
 * Онбординг v4.1: splash → policy → perms → welcome → main.
 * Splash уходит автоматически через 1800 мс, welcome — только по кнопке.
 */
@Composable
fun OnboardingFlow(onDone: () -> Unit) {
    var step by remember { mutableStateOf(OnbStep.Splash) }
    var agreed by remember { mutableStateOf(false) }
    val perms = remember { mutableStateMapOf("notify" to false, "storage" to false, "battery" to false) }

    Box(Modifier.fillMaxSize().background(Aurora.Surface)) {
        when (step) {
            OnbStep.Splash -> SplashScreen { step = OnbStep.Policy }
            OnbStep.Policy -> PolicyScreen(agreed, { agreed = it }) { step = OnbStep.Perms }
            OnbStep.Perms -> PermsScreen(perms) { step = OnbStep.Welcome }
            OnbStep.Welcome -> WelcomeScreen(onDone)
        }
    }
}

/* ---------------- 0.1 Splash ---------------- */

@Composable
private fun SplashScreen(onNext: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onNext()
    }
    val t = rememberInfiniteTransition(label = "logoBreath")
    val breath by t.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2500, easing = Aurora.Ease), RepeatMode.Reverse),
        label = "breath",
    )

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        /*
         * Логотип приложения.
         *
         * Раньше он рисовался скруглённым квадратом с собственным
         * тёмным фоном — на чёрном экране это читалось как наклейка
         * поверх, «неестественно вставленная картинка». Теперь ассет без
         * подложки (только арт на прозрачном), а вместо коробки под ним
         * мягкое свечение: логотип принадлежит экрану, а не лежит на нём.
         */
        Box(
            Modifier.size(148.dp).scale(breath),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Aurora.Acc.copy(alpha = 0.28f),
                        0.55f to Aurora.Acc.copy(alpha = 0.10f),
                        1f to Color.Transparent,
                    ),
                    radius = size.minDimension / 2f,
                )
            }
            Image(
                painter = painterResource(R.drawable.takami_logo),
                contentDescription = "Takami",
                modifier = Modifier.size(104.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("Takami", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.88).sp)
        Spacer(Modifier.height(6.dp))
        Text("高見 · 見る", color = Aurora.Acc2, fontSize = 15.sp, letterSpacing = 4.5.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            "Манга, аниме и ранобэ — в одном приложении, с общим прогрессом.",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.widthIn(max = 260.dp),
        )
        Spacer(Modifier.height(28.dp))
        SplashLoader()
    }
}

/* ---------------- 0.2 Policy ---------------- */

@Composable
private fun PolicyScreen(agreed: Boolean, onAgree: (Boolean) -> Unit, onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 44.dp, bottom = 24.dp)
    ) {
        ProgressDots(3, 1)
        Spacer(Modifier.height(20.dp))
        Text("Пара слов, прежде чем начнём", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Takami — открытый клиент. Мы уважаем вас и просим уважать наши условия.",
            color = Aurora.OnSurfaceVariant, fontSize = 13.sp, lineHeight = 21.sp,
        )
        Spacer(Modifier.height(18.dp))

        val cards = listOf(
            "Мы не хостим контент" to "Приложение — только инструмент просмотра. Права на контент принадлежат владельцам.",
            "Встроенный VPN — для удобства" to "Это прокси-клиент. Мы не логируем трафик, ключи хранятся только на устройстве.",
            "За контент отвечает источник" to "Если парсер сломался — это не наша вина; автопарсер попробует восстановиться сам.",
        )
        cards.forEach { (title, body) ->
            SubCard {
                Column {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(body, color = Color.White.copy(alpha = .72f), fontSize = 12.sp, lineHeight = 19.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Aurora.Acc.copy(alpha = .06f))
                .border(1.dp, Aurora.Acc.copy(alpha = .2f), RoundedCornerShape(14.dp))
                .clickable { onAgree(!agreed) }
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (agreed) Modifier.background(Aurora.AccentGradient)
                        else Modifier.border(1.5.dp, Aurora.BrdEm, RoundedCornerShape(6.dp))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (agreed) Icon(TakamiIcon.Check, Modifier.size(14.dp), Color.White)
            }
            Text(
                "Я прочитал(а) и согласен(-а). Претензий по контенту к приложению не имею.",
                color = Color.White.copy(alpha = .82f), fontSize = 12.sp, lineHeight = 19.sp,
            )
        }

        Spacer(Modifier.height(20.dp))
        GradientCta("Продолжить", enabled = agreed, onClick = onNext)
    }
}

/* ---------------- 0.3 Permissions ---------------- */

private data class Perm(val key: String, val icon: TakamiIcon, val title: String, val body: String)

@Composable
private fun PermsScreen(perms: MutableMap<String, Boolean>, onNext: () -> Unit) {
    val list = listOf(
        Perm("notify", TakamiIcon.Bell, "Уведомления", "Новые главы и эпизоды, статус загрузок"),
        Perm("storage", TakamiIcon.Folder, "Папка с контентом", "Откуда читать главы и куда сохранять скачанное"),
        Perm("battery", TakamiIcon.Battery, "Без экономии батареи", "Чтобы фоновые загрузки не обрывались"),
    )
    val context = LocalContext.current

    /*
     * Раньше нажатие просто ставило флаг в памяти: кнопка «выдавала»
     * разрешение, которого приложение никогда не получало. Системного
     * диалога при этом не появлялось вовсе — со стороны это и выглядит
     * как неработающая выдача. Теперь каждая карточка запускает
     * настоящий системный запрос.
     */
    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { perms["notify"] = it }

    /*
     * Хранилище — это НЕ runtime-разрешение начиная с Android 11:
     * запрос WRITE_EXTERNAL_STORAGE система игнорирует, и карточка
     * «выдавала» доступ, которого нет. Пользователю при этом нужен не
     * флаг, а ответ на вопрос «откуда читать и куда сохранять»,
     * поэтому карточка открывает выбор папки.
     */
    val root = remember { LibraryRoot(context) }
    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            root.select(uri)
            perms["storage"] = true
        }
    }

    // Отключение экономии батареи и доступ ко всем файлам — не runtime-
    // разрешения: их нельзя запросить диалогом, только увести в настройки.
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { perms["battery"] = context.isIgnoringBatteryOptimizations() }

    // Синхронизация с системой: пользователь мог выдать или отозвать
    // разрешение снаружи, и экран обязан показывать правду, а не память.
    LaunchedEffect(Unit) {
        perms["notify"] = context.hasNotificationPermission()
        perms["storage"] = root.selectedTree() != null
        perms["battery"] = context.isIgnoringBatteryOptimizations()
    }

    fun request(key: String) {
        when (key) {
            "notify" ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // До Android 13 разрешение выдаётся установкой приложения.
                    perms["notify"] = true
                }
            "storage" -> treeLauncher.launch(null)
            "battery" -> batteryLauncher.launch(context.batteryOptimizationIntent())
        }
    }

    val allGranted = list.all { perms[it.key] == true }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 44.dp, bottom = 24.dp)
    ) {
        ProgressDots(3, 2)
        Spacer(Modifier.height(20.dp))
        Text("Нужны разрешения", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Дайте согласие сейчас, потом настройки можно поменять в системе.",
            color = Aurora.OnSurfaceVariant, fontSize = 13.sp, lineHeight = 21.sp,
        )
        Spacer(Modifier.height(18.dp))

        list.forEach { p ->
            val granted = perms[p.key] == true
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x08FFFFFF))
                    .border(1.dp, Aurora.Brd, RoundedCornerShape(16.dp))
                    .clickable(enabled = !granted) { request(p.key) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (granted) Brush.linearGradient(listOf(Aurora.Ok, Color(0xFF24A566)))
                            else Brush.linearGradient(listOf(Aurora.Acc.copy(alpha = .22f), Aurora.AccDim.copy(alpha = .1f)))
                        )
                        .border(
                            1.dp,
                            if (granted) Color.Transparent else Aurora.Acc.copy(alpha = .28f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(p.icon, Modifier.size(22.dp), if (granted) Color.White else Aurora.Acc2)
                }
                Column(Modifier.weight(1f)) {
                    Text(p.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(p.body, color = Aurora.OnSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
                }
                /*
                 * Чип — самостоятельная кнопка, а не украшение. Раньше
                 * clickable висел только на строке целиком, и палец,
                 * попадающий точно по «Разрешить», не вызывал ничего:
                 * визуально это выглядело как неработающая выдача.
                 */
                Box(
                    Modifier
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(if (granted) Aurora.Ok.copy(alpha = .16f) else Aurora.Acc.copy(alpha = .16f))
                        .clickable(enabled = !granted) { request(p.key) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (granted) "Выдано" else "Разрешить",
                        color = if (granted) Aurora.Ok else Aurora.Acc2,
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(14.dp))
        GradientCta(if (allGranted) "Отлично, дальше" else "Пропустить и продолжить", onClick = onNext)
        if (!allGranted) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Некоторые функции могут работать некорректно",
                color = Aurora.OnSurfaceVariant, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/* ---------------- 0.4 Welcome ---------------- */

@Composable
private fun WelcomeScreen(onDone: () -> Unit) {
    val t = rememberInfiniteTransition(label = "welcome")
    val halo by t.animateFloat(
        initialValue = .94f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(3400, easing = Aurora.Ease), RepeatMode.Reverse),
        label = "halo",
    )
    val bubbleWiggle by t.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(3600, easing = Aurora.Ease), RepeatMode.Reverse),
        label = "wiggle",
    )
    val ctaGlow by t.animateFloat(
        initialValue = .85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = Aurora.Ease), RepeatMode.Reverse),
        label = "ctaPulse",
    )

    Box(Modifier.fillMaxSize()) {
        // Ореолы
        Box(
            Modifier
                .align(Alignment.Center)
                .size(340.dp)
                .scale(halo)
                .background(
                    Brush.radialGradient(
                        listOf(Aurora.Acc.copy(alpha = .55f), Color.Transparent),
                        radius = 480f,
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = 40.dp)
                .size(220.dp)
                .scale(2.04f - halo)
                .background(
                    Brush.radialGradient(
                        listOf(Aurora.HaloPink.copy(alpha = .35f), Color.Transparent),
                        radius = 320f,
                    )
                )
        )

        // Полупрозрачные иероглифы
        Text("お", color = Color.White.copy(alpha = .09f), fontSize = 220.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopStart).offset(x = (-18).dp, y = 40.dp))
        Text("帰", color = Color.White.copy(alpha = .09f), fontSize = 180.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = 110.dp))
        Text("り", color = Aurora.HaloPink.copy(alpha = .06f), fontSize = 160.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = 120.dp, y = (-160).dp))

        // Искры
        Sparkles(Modifier.fillMaxSize())

        /*
         * Иллюстрация. В прошлой сборке её не было вовсе: ассет
         * welcome-girl.png остался в хендоффе и не переехал в res —
         * экран выглядел пустым между иероглифами и кнопкой.
         * Анимация двухслойная, как в прототипе: дыхание всего блока
         * (girlBreath, 3.6 с) и покачивание самой картинки
         * (girlSway, ±1.2°, 4.8 с) с опорой по нижнему краю.
         */
        val girlBreath by t.animateFloat(
            initialValue = 1f, targetValue = 1.012f,
            animationSpec = infiniteRepeatable(tween(3600, easing = Aurora.Ease), RepeatMode.Reverse),
            label = "girlBreath",
        )
        val girlSway by t.animateFloat(
            initialValue = -1.2f, targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(4800, easing = Aurora.Ease), RepeatMode.Reverse),
            label = "girlSway",
        )
        Image(
            painter = painterResource(R.drawable.welcome_girl),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                // Выше и крупнее: на сборке персонаж стоял слишком низко,
                // между пузырьком и головой оставался пустой провал.
                .fillMaxHeight(0.82f)
                .offset(y = (-56).dp)
                .scale(girlBreath)
                .rotate(girlSway),
        )

        /*
         * Речевой пузырёк.
         *
         * Висел в правом верхнем углу с жёстким отступом сверху: хвостик
         * указывал в пустоту, а сам пузырёк наезжал на голову персонажа —
         * реплика читалась как чужая. Теперь он слева от головы, а
         * хвостик (скруглённый угол bottomEnd) направлен вправо-вниз,
         * то есть на говорящего.
         */
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(top = 172.dp, start = 20.dp)
                .widthIn(max = 190.dp)
                .scale(1f + bubbleWiggle * 0.004f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp))
                .background(Brush.linearGradient(listOf(Color.White, Color(0xFFF4EBFF))))
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)
        ) {
            Text("お帰りなさいませ", color = Aurora.Acc, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(4.dp))
            Text("Добро пожаловать, хозяин!", color = Color(0xFF0F1116), fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
        }

        // Футер: градиентная подложка + сообщение + CTA
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.25f to Aurora.Surface.copy(alpha = .55f),
                        0.7f to Aurora.Surface.copy(alpha = .92f),
                        1f to Aurora.Surface,
                    )
                )
                .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Всё готово. Приятного чтения.",
                color = Color.White.copy(alpha = .72f), fontSize = 13.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(16.dp))
            Box(Modifier.scale(0.98f + ctaGlow * 0.02f)) {
                GradientCta("Войти в приложение", onClick = onDone)
            }
        }
    }
}

/* ---------------- helpers ---------------- */

/** Летающие искры вокруг персонажа: 8 точек, sparkleFloat 5s, разные фазы. */
@Composable
private fun Sparkles(modifier: Modifier = Modifier) {
    data class Spark(val x: Float, val y: Float, val size: Int, val color: Color, val delayMs: Int)

    val sparks = remember {
        listOf(
            Spark(.18f, .12f, 4, Color.White, 0),
            Spark(.86f, .20f, 4, Color(0xFFFFB0D0), 700),
            Spark(.08f, .34f, 4, Color.White, 1400),
            Spark(.92f, .42f, 4, Color.White, 2100),
            Spark(.22f, .55f, 3, Color.White, 2800),
            Spark(.80f, .62f, 4, Color.White, 3500),
            Spark(.45f, .08f, 5, Aurora.Acc2, 4200),
            Spark(.68f, .28f, 3, Color.White, 1000),
        )
    }
    val t = rememberInfiniteTransition(label = "sparkles")

    Box(modifier) {
        sparks.forEach { sp ->
            val phase by t.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(5000, easing = Aurora.Ease),
                    RepeatMode.Restart,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(sp.delayMs),
                ),
                label = "spark",
            )
            // opacity: 0 -> 1 (20%) -> 1 (80%) -> 0; смещение вверх-влево
            val alpha = when {
                phase < .2f -> phase / .2f
                phase > .8f -> (1f - phase) / .2f
                else -> 1f
            }
            val scale = if (phase < .5f) .4f + phase * 1.2f else 1f - (phase - .5f) * 1.2f
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .offset(
                            x = maxWidth * sp.x - (phase * 12).dp,
                            y = maxHeight * sp.y - (phase * 36).dp,
                        )
                        .size(sp.size.dp)
                        .scale(scale.coerceIn(.3f, 1f))
                        .alpha(alpha.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(Aurora.RadiusFull))
                        .background(sp.color)
                )
            }
        }
    }
}



/* ---------------- системные разрешения ---------------- */

private fun Context.hasNotificationPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    else true

private fun Context.hasStoragePermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) true
    else ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

private fun Context.isIgnoringBatteryOptimizations(): Boolean = runCatching {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    pm.isIgnoringBatteryOptimizations(packageName)
}.getOrDefault(false)

/**
 * Прямой диалог отключения экономии доступен не на всех прошивках, и на
 * части из них запуск падает с ActivityNotFoundException. Поэтому здесь
 * же готовится запасной путь — общий экран настроек приложения.
 */
private fun Context.batteryOptimizationIntent(): Intent {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:$packageName"))
    val resolvable = packageManager.resolveActivity(direct, 0) != null
    return if (resolvable) direct
    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:$packageName"))
}
