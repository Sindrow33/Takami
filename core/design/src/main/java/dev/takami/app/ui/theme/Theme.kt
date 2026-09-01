package dev.takami.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val TakamiColors = darkColorScheme(
    primary = Aurora.Primary,
    onPrimary = Aurora.OnPrimary,
    secondary = Aurora.Acc3,
    tertiary = Aurora.AccBlue,
    background = Aurora.Surface,
    onBackground = Aurora.OnSurface,
    surface = Aurora.Surface,
    onSurface = Aurora.OnSurface,
    surfaceVariant = Aurora.SurfaceVariant,
    onSurfaceVariant = Aurora.OnSurfaceVariant,
    surfaceContainer = Aurora.SurfaceContainer,
    outline = Aurora.Outline,
    outlineVariant = Aurora.OutlineVar,
    error = Aurora.Error,
)

/**
 * Тип-шкала. В прототипе display-шрифт — Zen Kaku Gothic Antique;
 * пока шрифтовые ресурсы не залиты, используется системный sans с теми же
 * весами и метриками, чтобы вёрстка совпадала по размерам.
 */
private val TakamiTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = (-0.88).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)

@Composable
fun TakamiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TakamiColors,
        typography = TakamiTypography,
        content = content,
    )
}
