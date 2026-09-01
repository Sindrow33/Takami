package dev.takami.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Aurora design tokens — 1:1 перенос из design_handoff_takami_v4 / colors_and_type.css.
 * Dark-first. Значения финальные, менять только вместе с дизайн-системой.
 */
object Aurora {
    // ---------- Акценты ----------
    val Acc = Color(0xFF7C5CFF)
    val AccDim = Color(0xFF5B3BE8)
    val Acc2 = Color(0xFFA78BFA)
    val Acc3 = Color(0xFF00E5FF)
    val AccBlue = Color(0xFF0095FF)
    val GradA = Color(0xFF8E72FF)
    val GradB = Color(0xFF5B3BE8)

    val Primary = Acc
    val OnPrimary = Color(0xFFFFFFFF)

    // ---------- Семафор ----------
    val Ok = Color(0xFF3DD68C)
    val Warn = Color(0xFFFFB020)
    val Error = Color(0xFFF87171)

    // ---------- Поверхности ----------
    val Surface = Color(0xFF0F1116)
    val SurfaceContainer = Color(0xFF1A1D23)
    val SurfaceVariant = Color(0xFF252931)
    val ScLowest = Color(0xFF0A0C0F)
    val ScLow = Color(0xFF13151A)

    // ---------- Текст ----------
    val OnSurface = Color(0xFFFFFFFF)
    val OnSurfaceVariant = Color(0xFF94A3B8)

    // ---------- Обводки / стекло ----------
    val Outline = Color(0xFF334155)
    val OutlineVar = Color(0xFF1E293B)
    val Sub = Color(0x0DFFFFFF)      // rgba(255,255,255,.05)
    val Brd = Color(0x14FFFFFF)      // rgba(255,255,255,.08)
    val BrdEm = Color(0x29FFFFFF)    // rgba(255,255,255,.16)

    // ---------- Семантика типов контента ----------
    val TypeAnime = AccBlue
    val TypeManga = Ok
    val TypeNovel = Acc2
    val HeartPink = Color(0xFFFF6B8A)
    val HaloPink = Color(0xFFFF96BE)

    // ---------- Градиенты ----------
    val AccentGradient = Brush.linearGradient(listOf(GradA, GradB))
    val AtmosphereGradient = Brush.linearGradient(listOf(Color(0xFF1E1B4B), Surface))

    // ---------- Радиусы ----------
    val RadiusS = 8.dp
    val RadiusM = 12.dp
    val RadiusL = 20.dp
    val RadiusFull = 999.dp

    // ---------- Motion ----------
    const val DurFast = 140
    const val DurMid = 240
    const val DurSlow = 420
    val Ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseBounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
}
