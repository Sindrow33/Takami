package dev.takami.app.parser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.takami.app.ui.theme.Aurora
import kotlinx.coroutines.launch

/**
 * Разбор сайта по ссылке.
 *
 * Экран для того, что до сих пор было доступно только из кода: дать
 * автопарсеру адрес страницы и посмотреть, что он с неё снял. Нужен и
 * пользователю (добавить свой источник), и нам — это единственный
 * способ увидеть на устройстве, работает ли парсер на живом сайте.
 */
@Composable
fun AutoParseScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prober = remember { SourceProber(context) }

    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SourceProber.Report?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Aurora.Surface)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
    ) {
        Text(
            "‹ Назад",
            color = Aurora.OnSurfaceVariant,
            fontSize = 15.sp,
            modifier = Modifier.clickable(onClick = onClose).padding(vertical = 6.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text("Разбор сайта", color = Aurora.OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Вставьте ссылку на страницу тайтла или каталога. Парсер снимет " +
                "разметку и покажет, что нашёл; при поломке селекторов он " +
                "попробует починить их сам.",
            color = Aurora.OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0FFFFFFF))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            if (url.isEmpty()) {
                Text("https://…", color = Aurora.OnSurfaceVariant.copy(alpha = .6f), fontSize = 14.sp)
            }
            BasicTextField(
                value = url,
                onValueChange = { url = it.trim() },
                singleLine = true,
                textStyle = TextStyle(color = Aurora.OnSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(Aurora.Acc2),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Aurora.RadiusFull))
                .then(
                    if (!busy && url.startsWith("http")) Modifier.background(Aurora.AccentGradient)
                    else Modifier.background(Color(0x14FFFFFF))
                )
                .clickable(enabled = !busy && url.startsWith("http")) {
                    busy = true
                    result = null
                    scope.launch {
                        result = prober.probe(url)
                        busy = false
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (busy) "Разбираю…" else "Разобрать",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        result?.let { report ->
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x0AFFFFFF))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    if (report.ok) "Разобрано" else "Не получилось",
                    color = if (report.ok) Aurora.Acc2 else Color(0xFFFF6B6B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                report.lines.forEach { line ->
                    Text(line, color = Aurora.OnSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}
