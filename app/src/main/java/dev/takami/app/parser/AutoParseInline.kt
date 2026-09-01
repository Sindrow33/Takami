package dev.takami.app.parser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
 * Поле адреса и кнопка разбора — компактный блок, встраиваемый в чужой
 * экран.
 *
 * Раньше это был отдельный экран, открывавшийся и с главной, и из
 * настроек. Оказалось, что разбор сайта — не самостоятельный экран, а
 * действие внутри карточки автопарсера: пользователь открывает сводку
 * движка и там же добавляет источник. Два входа в одно действие только
 * делили внимание, поэтому экран убран, а блок встраивается в сводку.
 */
@Composable
fun AutoParseInline(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prober = remember { SourceProber(context) }

    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SourceProber.Report?>(null) }
    val ready = !busy && url.startsWith("http")

    Column(modifier.fillMaxWidth()) {
        Text(
            "Разбор сайта",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Вставьте ссылку на каталог, страницу тайтла или главу — движок " +
                "сам подберёт селекторы и запомнит их.",
            color = Aurora.OnSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(10.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0FFFFFFF))
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            if (url.isEmpty()) {
                Text("https://…", color = Aurora.OnSurfaceVariant.copy(alpha = .6f), fontSize = 13.sp)
            }
            BasicTextField(
                value = url,
                onValueChange = { url = it.trim() },
                singleLine = true,
                textStyle = TextStyle(color = Aurora.OnSurface, fontSize = 13.sp),
                cursorBrush = SolidColor(Aurora.Acc2),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Aurora.RadiusFull))
                .then(
                    if (ready) Modifier.background(Aurora.AccentGradient)
                    else Modifier.background(Color(0x14FFFFFF))
                )
                .clickable(enabled = ready) {
                    busy = true
                    result = null
                    scope.launch {
                        result = prober.probe(url)
                        busy = false
                    }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (busy) "Разбираю…" else "Разобрать",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        result?.let { report ->
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0AFFFFFF))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    if (report.ok) "Разобрано" else "Не получилось",
                    color = if (report.ok) Aurora.Acc2 else Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                report.lines.forEach { line ->
                    Text(line, color = Aurora.OnSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
