package dev.takami.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Иконки набора Takami. В прототипе — inline SVG (stroke 1.8, round).
 * Здесь тот же контур на Canvas, координаты в сетке 24x24.
 */
enum class TakamiIcon { Home, Library, Calendar, Settings, Search, Swipes, Brain, Play, Book, Bell, Folder, Battery, Check, Heart }

@Composable
fun Icon(
    icon: TakamiIcon,
    modifier: Modifier = Modifier.size(22.dp),
    tint: Color = Color.White,
) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        val stroke = Stroke(width = 1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (icon) {
            TakamiIcon.Home -> {
                val path = Path().apply {
                    moveTo(3f * u, 10f * u); lineTo(12f * u, 3f * u); lineTo(21f * u, 10f * u)
                    lineTo(21f * u, 20f * u); lineTo(3f * u, 20f * u); close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, p(9f, 20f), p(9f, 14f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(15f, 20f), p(15f, 14f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(9f, 14f), p(15f, 14f), stroke.width, StrokeCap.Round)
            }
            TakamiIcon.Library -> {
                listOf(4f, 10f, 16f).forEach { x ->
                    drawLine(tint, p(x, 4f), p(x, 20f), stroke.width, StrokeCap.Round)
                    drawLine(tint, p(x + 4f, 4f), p(x + 4f, 20f), stroke.width, StrokeCap.Round)
                    drawLine(tint, p(x, 4f), p(x + 4f, 4f), stroke.width, StrokeCap.Round)
                    drawLine(tint, p(x, 20f), p(x + 4f, 20f), stroke.width, StrokeCap.Round)
                }
            }
            TakamiIcon.Calendar -> {
                val path = Path().apply {
                    moveTo(4f * u, 6f * u); lineTo(20f * u, 6f * u)
                    lineTo(20f * u, 20f * u); lineTo(4f * u, 20f * u); close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, p(4f, 10f), p(20f, 10f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(8f, 3.5f), p(8f, 6f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(16f, 3.5f), p(16f, 6f), stroke.width, StrokeCap.Round)
            }
            TakamiIcon.Settings -> {
                drawCircle(tint, radius = 3.2f * u, center = p(12f, 12f), style = stroke)
                for (i in 0 until 8) {
                    val a = Math.toRadians((i * 45).toDouble())
                    val cx = 12f + 7.4f * kotlin.math.cos(a).toFloat()
                    val cy = 12f + 7.4f * kotlin.math.sin(a).toFloat()
                    val ix = 12f + 5.2f * kotlin.math.cos(a).toFloat()
                    val iy = 12f + 5.2f * kotlin.math.sin(a).toFloat()
                    drawLine(tint, p(ix, iy), p(cx, cy), stroke.width, StrokeCap.Round)
                }
            }
            TakamiIcon.Search -> {
                drawCircle(tint, radius = 6.4f * u, center = p(10.5f, 10.5f), style = stroke)
                drawLine(tint, p(15.4f, 15.4f), p(20.5f, 20.5f), stroke.width, StrokeCap.Round)
            }
            TakamiIcon.Swipes -> {
                // стрелки-круг: две дуги со наконечниками
                drawArc(tint, 200f, 140f, false, topLeft = p(4f, 4f), size = androidx.compose.ui.geometry.Size(16f * u, 16f * u), style = stroke)
                drawArc(tint, 20f, 140f, false, topLeft = p(4f, 4f), size = androidx.compose.ui.geometry.Size(16f * u, 16f * u), style = stroke)
                drawLine(tint, p(6.5f, 6.5f), p(6.5f, 10.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(6.5f, 6.5f), p(10.5f, 6.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(17.5f, 17.5f), p(17.5f, 13.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(17.5f, 17.5f), p(13.5f, 17.5f), stroke.width, StrokeCap.Round)
            }
            TakamiIcon.Brain -> {
                drawCircle(tint, radius = 5.6f * u, center = p(12f, 12f), style = stroke)
                drawLine(tint, p(12f, 6.4f), p(12f, 17.6f), stroke.width, StrokeCap.Round)
                drawArc(tint, 90f, 180f, false, topLeft = p(7.6f, 7.5f), size = androidx.compose.ui.geometry.Size(4.4f * u, 4.5f * u), style = stroke)
                drawArc(tint, 270f, 180f, false, topLeft = p(12f, 12f), size = androidx.compose.ui.geometry.Size(4.4f * u, 4.5f * u), style = stroke)
            }
            TakamiIcon.Play -> {
                val path = Path().apply {
                    moveTo(8f * u, 5f * u); lineTo(19f * u, 12f * u); lineTo(8f * u, 19f * u); close()
                }
                drawPath(path, tint)
            }
            TakamiIcon.Book -> {
                val path = Path().apply {
                    moveTo(4f * u, 5f * u); lineTo(11f * u, 5f * u); lineTo(11f * u, 20f * u); lineTo(4f * u, 20f * u); close()
                    moveTo(13f * u, 5f * u); lineTo(20f * u, 5f * u); lineTo(20f * u, 20f * u); lineTo(13f * u, 20f * u); close()
                }
                drawPath(path, tint, style = stroke)
            }
            TakamiIcon.Bell -> {
                val path = Path().apply {
                    moveTo(6f * u, 17f * u); lineTo(6f * u, 11f * u)
                    cubicTo(6f * u, 7f * u, 8.5f * u, 5f * u, 12f * u, 5f * u)
                    cubicTo(15.5f * u, 5f * u, 18f * u, 7f * u, 18f * u, 11f * u)
                    lineTo(18f * u, 17f * u); close()
                }
                drawPath(path, tint, style = stroke)
                drawArc(tint, 0f, 180f, false, topLeft = p(10f, 17f), size = androidx.compose.ui.geometry.Size(4f * u, 3.6f * u), style = stroke)
            }
            TakamiIcon.Folder -> {
                val path = Path().apply {
                    moveTo(3.5f * u, 7f * u); lineTo(10f * u, 7f * u); lineTo(11.6f * u, 9.2f * u)
                    lineTo(20.5f * u, 9.2f * u); lineTo(20.5f * u, 19f * u); lineTo(3.5f * u, 19f * u); close()
                }
                drawPath(path, tint, style = stroke)
            }
            TakamiIcon.Battery -> {
                val path = Path().apply {
                    moveTo(3f * u, 8f * u); lineTo(18f * u, 8f * u); lineTo(18f * u, 16f * u); lineTo(3f * u, 16f * u); close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, p(20f, 10.5f), p(20f, 13.5f), 2.6f * u, StrokeCap.Round)
            }
            TakamiIcon.Check -> {
                drawLine(tint, p(5f, 12.6f), p(10f, 17.4f), 3f * u, StrokeCap.Round)
                drawLine(tint, p(10f, 17.4f), p(19f, 7f), 3f * u, StrokeCap.Round)
            }
            TakamiIcon.Heart -> {
                val path = Path().apply {
                    moveTo(12f * u, 20f * u)
                    cubicTo(4f * u, 14.5f * u, 3f * u, 10.5f * u, 5.4f * u, 7.6f * u)
                    cubicTo(7.6f * u, 5f * u, 10.6f * u, 6f * u, 12f * u, 8.6f * u)
                    cubicTo(13.4f * u, 6f * u, 16.4f * u, 5f * u, 18.6f * u, 7.6f * u)
                    cubicTo(21f * u, 10.5f * u, 20f * u, 14.5f * u, 12f * u, 20f * u)
                    close()
                }
                drawPath(path, tint)
            }
        }
    }
}
