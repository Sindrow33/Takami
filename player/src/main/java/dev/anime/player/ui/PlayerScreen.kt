package dev.anime.player.ui

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.anime.player.core.Media3Engine
import dev.anime.player.skip.SkipSegment
import dev.anime.player.skip.activeSegmentAt
import dev.anime.player.track.TrackKind
import kotlinx.coroutines.delay

/**
 * Экран воспроизведения. Раскладка жестов и панели сверена с Tadami/mpvKt:
 * двойной тап по краям — перемотка с накоплением, центр — пауза, вертикальный
 * свайп слева — яркость, справа — громкость, горизонтальный свайп — перемотка
 * с предпросмотром.
 *
 * Вся арифметика жестов живёт в [PlayerGestures] и покрыта тестами; здесь
 * только жесты, отрисовка и обращения к системе.
 */
@Composable
fun PlayerScreen(
    engine: Media3Engine,
    segments: List<SkipSegment>,
    autoSkip: Boolean = false,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    /** Включена ли ИИ-озвучка; null-обработчик скрывает кнопку целиком. */
    dubbingEnabled: Boolean = false,
    dubbingLoading: Boolean = false,
    onToggleDubbing: (() -> Unit)? = null,
    /** Короткое сообщение поверх видео (результат включения озвучки, причина отказа). */
    notice: String? = null,
    onNoticeDismiss: () -> Unit = {},
    /** Подпись кнопки перехода к следующей серии; null — кнопки нет. */
    nextEpisodeLabel: String? = null,
    onNextEpisode: (() -> Unit)? = null,
) {
    val state by engine.state.collectAsState()
    val context = LocalContext.current

    var controlsVisible by remember { mutableStateOf(true) }
    var autoSkipped by remember { mutableStateOf<SkipSegment?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var size by remember { mutableStateOf(0f to 0f) }
    var speed by remember { mutableFloatStateOf(1f) }

    // Накопленная перемотка двойным тапом и её плашка.
    var seekAccum by remember { mutableLongStateOf(0L) }
    // Предпросмотр позиции при горизонтальном свайпе: пока тянут, показываем
    // будущую позицию, а не текущую — иначе цифры отстают от пальца.
    var scrubPreviewMs by remember { mutableStateOf<Long?>(null) }

    // Оверлеи яркости и громкости.
    var tracksVisible by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf(engine.availableTracks()) }

    // Список дорожек готов не сразу после load(), а после разбора контейнера —
    // поэтому не читаем его один раз, а слушаем изменения.
    DisposableEffect(engine) {
        engine.onTracksChanged { tracks = it }
        onDispose { }
    }

    var brightness by remember { mutableFloatStateOf(currentBrightness(context)) }
    var volume by remember { mutableFloatStateOf(currentVolumeFraction(context)) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }

    // Автоскрытие панели: только во время воспроизведения. На паузе панель
    // должна оставаться — иначе на паузе невозможно попасть по кнопке.
    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    LaunchedEffect(seekAccum) {
        if (seekAccum != 0L) {
            delay(PlayerGestures.SEEK_LABEL_TIMEOUT_MS)
            seekAccum = 0L
        }
    }

    LaunchedEffect(overlay) {
        if (overlay != null) {
            delay(900)
            overlay = null
        }
    }

    // Яркость — свойство окна, а не плеера: возвращаем системную при выходе,
    // иначе выбранная в плеере яркость останется на всём приложении.
    DisposableEffect(Unit) {
        onDispose { applyBrightness(context, -1f) }
    }

    val active = remember(segments, state.positionMs) {
        activeSegmentAt(segments, state.positionMs)
    }

    LaunchedEffect(active, autoSkip) {
        val seg = active
        if (autoSkip && seg != null && seg != autoSkipped) {
            autoSkipped = seg
            engine.seekTo(seg.endMs)
            toast = seg.label.replace("Пропустить", "Пропущен")
            delay(4000)
            toast = null
        }
    }

    Box(
        modifier
            .background(Color.Black)
            .onSizeChanged { size = it.width.toFloat() to it.height.toFloat() },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    player = engine.exoInstance()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .fillMaxSize()
                // Тапы: одиночный — панель, двойной — перемотка или пауза.
                .pointerInput(size) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { offset ->
                            when (PlayerGestures.zoneAt(offset.x, size.first)) {
                                PlayerGestures.Zone.Center ->
                                    if (state.isPlaying) engine.pause() else engine.play()
                                PlayerGestures.Zone.Right -> {
                                    seekAccum = PlayerGestures.accumulateSeek(seekAccum, true)
                                    engine.seekTo(
                                        PlayerGestures.seekTarget(
                                            state.positionMs,
                                            PlayerGestures.SEEK_STEP_MS,
                                            state.durationMs,
                                        )
                                    )
                                }
                                PlayerGestures.Zone.Left -> {
                                    seekAccum = PlayerGestures.accumulateSeek(seekAccum, false)
                                    engine.seekTo(
                                        PlayerGestures.seekTarget(
                                            state.positionMs,
                                            -PlayerGestures.SEEK_STEP_MS,
                                            state.durationMs,
                                        )
                                    )
                                }
                            }
                        },
                        onLongPress = {
                            // Удержание — ускорение до 2x, как в Tadami. Слайд по
                            // пресетам не переносим: на телефоне он конфликтует с
                            // горизонтальной перемоткой, а выигрыш от него мал.
                            engine.setSpeed(PlayerGestures.LONG_PRESS_SPEED)
                            overlay = Overlay.Speed(PlayerGestures.LONG_PRESS_SPEED)
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (speed != PlayerGestures.LONG_PRESS_SPEED) engine.setSpeed(speed)
                        },
                    )
                }
                // Вертикальные свайпы: слева яркость, справа громкость.
                .pointerInput(size) {
                    var startY = 0f
                    var startValue = 0f
                    var isBrightness = false
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            startY = offset.y
                            isBrightness = offset.x < size.first / 2f
                            startValue = if (isBrightness) brightness else volume
                        },
                        onDragEnd = { },
                    ) { change, _ ->
                        val value = PlayerGestures.verticalValue(
                            startValue, startY, change.position.y, size.second,
                        )
                        if (isBrightness) {
                            brightness = value
                            applyBrightness(context, value)
                            overlay = Overlay.Brightness(value)
                        } else {
                            volume = value
                            applyVolume(context, value)
                            overlay = Overlay.Volume(value)
                        }
                    }
                }
                // Горизонтальный свайп — перемотка с предпросмотром.
                .pointerInput(size) {
                    var startX = 0f
                    var startPos = 0L
                    var wasPlaying = false
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            startPos = state.positionMs
                            wasPlaying = state.isPlaying
                            // Пауза на время жеста: иначе позиция уезжает под
                            // пальцем и перемотка бьётся с воспроизведением.
                            if (wasPlaying) engine.pause()
                        },
                        onDragEnd = {
                            scrubPreviewMs?.let { engine.seekTo(it) }
                            scrubPreviewMs = null
                            if (wasPlaying) engine.play()
                        },
                        onDragCancel = {
                            scrubPreviewMs = null
                            if (wasPlaying) engine.play()
                        },
                    ) { change, _ ->
                        scrubPreviewMs = PlayerGestures.horizontalSeek(
                            startPos, startX, change.position.x, state.durationMs,
                        )
                    }
                }
        )

        if (state.isBuffering && state.error == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }

        SeekZoneLabel(seekAccum, Modifier.align(Alignment.Center))
        ScrubPreview(scrubPreviewMs, state.durationMs, Modifier.align(Alignment.Center))
        OverlayBadge(overlay, Modifier.align(Alignment.CenterStart))

        val seg = active
        if (seg != null && !autoSkip) {
            SkipButton(
                label = seg.label,
                onClick = { engine.seekTo(seg.endMs) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 96.dp),
            )
        }

        val msg = toast
        if (msg != null) {
            UndoToast(
                message = msg,
                onUndo = {
                    autoSkipped?.let { engine.seekTo(it.startMs) }
                    toast = null
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            )
        }

        if (nextEpisodeLabel != null && onNextEpisode != null) {
            SkipButton(
                label = nextEpisodeLabel,
                onClick = onNextEpisode,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 96.dp),
            )
        }

        if (notice != null) {
            Notice(
                text = notice,
                onDismiss = onNoticeDismiss,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
            )
        }

        if (controlsVisible) {
            TopBar(
                title = title,
                speed = speed,
                onBack = onBack,
                dubbingEnabled = dubbingEnabled,
                dubbingLoading = dubbingLoading,
                onToggleDubbing = onToggleDubbing,
                onTracks = if (hasTrackChoice(tracks)) {
                    { tracksVisible = true }
                } else {
                    null
                },
                onSpeed = {
                    speed = PlayerGestures.nextSpeed(speed)
                    engine.setSpeed(speed)
                    overlay = Overlay.Speed(speed)
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            ControlsBar(
                state = state,
                onPlayPause = { if (state.isPlaying) engine.pause() else engine.play() },
                onSeek = { engine.seekTo(it) },
                onStepBack = {
                    engine.seekTo(
                        PlayerGestures.seekTarget(
                            state.positionMs, -PlayerGestures.SEEK_STEP_MS, state.durationMs,
                        )
                    )
                },
                onStepForward = {
                    engine.seekTo(
                        PlayerGestures.seekTarget(
                            state.positionMs, PlayerGestures.SEEK_STEP_MS, state.durationMs,
                        )
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (tracksVisible) {
            TrackSheet(
                tracks = tracks,
                onSelect = { engine.selectTrack(it); tracks = engine.availableTracks() },
                onDisableSubtitles = {
                    engine.disableTracks(TrackKind.Subtitle)
                    tracks = engine.availableTracks()
                },
                onDismiss = { tracksVisible = false },
            )
        }
    }
}

private sealed interface Overlay {
    data class Volume(val value: Float) : Overlay
    data class Brightness(val value: Float) : Overlay
    data class Speed(val value: Float) : Overlay
}

@Composable
private fun SeekZoneLabel(accumMs: Long, modifier: Modifier) {
    val alpha by animateFloatAsState(if (accumMs == 0L) 0f else 1f, label = "seek")
    if (alpha <= 0.01f) return
    Box(
        modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            PlayerGestures.seekLabel(accumMs),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ScrubPreview(previewMs: Long?, durationMs: Long, modifier: Modifier) {
    if (previewMs == null) return
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            formatTime(previewMs) + " / " + formatTime(durationMs),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OverlayBadge(overlay: Overlay?, modifier: Modifier) {
    if (overlay == null) return
    val (label, fraction) = when (overlay) {
        is Overlay.Volume -> "Звук " + PlayerGestures.percent(overlay.value) + "%" to overlay.value
        is Overlay.Brightness -> "Яркость " + PlayerGestures.percent(overlay.value) + "%" to overlay.value
        is Overlay.Speed -> PlayerGestures.speedLabel(overlay.value) to 1f
    }
    Column(
        modifier
            .padding(start = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (overlay !is Overlay.Speed) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .width(4.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String?,
    speed: Float,
    onBack: (() -> Unit)?,
    onSpeed: () -> Unit,
    modifier: Modifier,
    dubbingEnabled: Boolean = false,
    dubbingLoading: Boolean = false,
    onToggleDubbing: (() -> Unit)? = null,
    onTracks: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            PillButton("‹", onBack)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = title.orEmpty(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (onTracks != null) {
            PillButton("CC", onTracks)
            Spacer(Modifier.width(8.dp))
        }
        if (onToggleDubbing != null) {
            PillButton(
                label = if (dubbingLoading) "…" else "ИИ",
                onClick = onToggleDubbing,
                active = dubbingEnabled,
            )
            Spacer(Modifier.width(8.dp))
        }
        PillButton(PlayerGestures.speedLabel(speed), onSpeed)
    }
}

@Composable
private fun Notice(text: String, onDismiss: () -> Unit, modifier: Modifier) {
    LaunchedEffect(text) {
        delay(5000)
        onDismiss()
    }
    Box(
        modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .pointerInput(text) { detectTapGestures { onDismiss() } }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit, active: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (active) Color(0xFF7C4DFF).copy(alpha = 0.85f)
                else Color.White.copy(alpha = 0.14f)
            )
            .size(width = 46.dp, height = 32.dp)
            .pointerInput(label) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SkipButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .pointerInput(label) { detectTapGestures { onClick() } }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) { Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun UndoToast(message: String, onUndo: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "ВЕРНУТЬ",
            color = Color(0xFF9C7CFF),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.pointerInput(Unit) { detectTapGestures { onUndo() } },
        )
    }
}

private fun audioManager(context: Context): AudioManager? =
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

private fun currentVolumeFraction(context: Context): Float {
    val am = audioManager(context) ?: return 0.5f
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (max <= 0) return 0.5f
    return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun applyVolume(context: Context, fraction: Float) {
    val am = audioManager(context) ?: return
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val steps = PlayerGestures.volumeSteps(fraction, max)
    runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, steps, 0) }
}

/** Текущая яркость окна; отрицательное значение означает «системная». */
private fun currentBrightness(context: Context): Float {
    val window = (context as? Activity)?.window ?: return 0.5f
    val value = window.attributes.screenBrightness
    return if (value < 0f) 0.5f else value
}

private fun applyBrightness(context: Context, fraction: Float) {
    val window = (context as? Activity)?.window ?: return
    runCatching {
        window.attributes = (window.attributes as WindowManager.LayoutParams).apply {
            screenBrightness = fraction
        }
    }
}
