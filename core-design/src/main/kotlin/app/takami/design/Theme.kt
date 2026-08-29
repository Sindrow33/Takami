package app.takami.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Акценты: фиолетовый — первичный, циан и синий — вторичные (см. tokens/aurora.css). */
object Acc {
    val Violet = Color(0xFF7C5CFF)
    val VioletDim = Color(0xFF5B3BE8)
    val VioletSoft = Color(0xFFA78BFA)
    val Cyan = Color(0xFF00E5FF)
    val Blue = Color(0xFF0095FF)
    val Ok = Color(0xFF3DD68C)
    val Warn = Color(0xFFFFB020)
}

private val Dark = darkColorScheme(
    primary = Acc.Violet, onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2050), onPrimaryContainer = Acc.VioletSoft,
    secondary = Acc.Cyan, onSecondary = Color.Black,
    tertiary = Acc.Blue, onTertiary = Color.White,
    background = Color(0xFF0F1116), onBackground = Color.White,
    surface = Color(0xFF1A1D23), onSurface = Color.White,
    surfaceVariant = Color(0xFF252931), onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0A0C0F),
    surfaceContainerLow = Color(0xFF13151A),
    surfaceContainer = Color(0xFF1A1D23),
    surfaceContainerHigh = Color(0xFF24272E),
    surfaceContainerHighest = Color(0xFF2F3239),
    outline = Color(0xFF334155), outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171), onError = Color.White,
)

private val Light = lightColorScheme(
    primary = Acc.VioletDim, onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E4FF), onPrimaryContainer = Color(0xFF2A1B6B),
    secondary = Color(0xFF0891B2), onSecondary = Color.White,
    tertiary = Color(0xFF0076CB), onTertiary = Color.White,
    background = Color(0xFFF8FAFC), onBackground = Color(0xFF0F172A),
    surface = Color.White, onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = Color(0xFFCBD5E1), outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626), onError = Color.White,
)

/** То, чего нет в MaterialTheme: градиент шапки, стекло, свечение. */
data class AuroraExtras(
    val isDark: Boolean,
    val heroGradient: Brush,
    val glass: Color,
    val border: Color,
    val borderStrong: Color,
    val ok: Color,
    val warn: Color,
)

val LocalAurora = staticCompositionLocalOf<AuroraExtras> { error("нет AuroraTheme") }

object Dim {
    val s1 = 4.dp; val s2 = 8.dp; val s3 = 12.dp; val s4 = 16.dp
    val s6 = 24.dp; val s8 = 32.dp
    val rS = 8.dp; val rM = 12.dp; val rL = 20.dp
    val cover = 104.dp
    val bar = 72.dp
}

@Composable
fun TakamiTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extras = AuroraExtras(
        isDark = dark,
        heroGradient = if (dark) {
            Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F1116)))
        } else {
            Brush.linearGradient(listOf(Color(0xFFE7E3FF), Color(0xFFF8FAFC)))
        },
        glass = if (dark) Color.White.copy(alpha = 0.05f) else Color.White,
        border = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
        borderStrong = if (dark) Color.White.copy(alpha = 0.16f) else Acc.Violet.copy(alpha = 0.28f),
        ok = if (dark) Acc.Ok else Color(0xFF16A34A),
        warn = if (dark) Acc.Warn else Color(0xFFD97706),
    )
    CompositionLocalProvider(LocalAurora provides extras) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
    }
}
