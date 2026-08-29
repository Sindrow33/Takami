package app.takami.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// значения один в один из tokens.css
object T {
    val surface          = Color(0xFF0F1115)
    val surfaceContainer = Color(0xFF181B21)
    val surfaceVariant   = Color(0xFF232833)
    val onSurface        = Color(0xFFE6E8EC)
    val onSurfaceVariant = Color(0xFF9AA1AE)
    val primary          = Color(0xFF7C5CFF)
    val primaryLight     = Color(0xFFA98BFF)
    val onPrimary        = Color(0xFFFFFFFF)
    val error            = Color(0xFFFF5C5C)
    val warning          = Color(0xFFFFB020)
    val success          = Color(0xFF3DD68C)
    val outline          = Color(0xFF2E343F)
}

object Sp { val x1 = 4.dp; val x2 = 8.dp; val x3 = 12.dp; val x4 = 16.dp; val x6 = 24.dp }
object R  { val s = 8.dp;  val m = 12.dp; val l = 20.dp }

private val scheme = darkColorScheme(
    primary = T.primary, onPrimary = T.onPrimary,
    background = T.surface, onBackground = T.onSurface,
    surface = T.surface, onSurface = T.onSurface,
    surfaceVariant = T.surfaceVariant, onSurfaceVariant = T.onSurfaceVariant,
    error = T.error, outline = T.outline
)

@Composable
fun TakamiTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, content = content)
