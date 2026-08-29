package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Панель источника. Жёлтая — автоподбор ушёл на источник послабее,
 * красная — живых источников не осталось, фиолетовая — закреплён вручную.
 */
@Composable
fun SourceBar(
    current: FakeSource?,
    best: FakeSource,
    pinned: Boolean,
    onChange: () -> Unit,
) {
    val a = LocalAurora.current
    val lost = if (current != null) best.maxChapter - current.maxChapter else 0
    val dot = when {
        current == null -> MaterialTheme.colorScheme.error
        pinned -> MaterialTheme.colorScheme.primary
        lost > 0 -> a.warn
        else -> a.ok
    }
    AuroraSurface(
        Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s3),
        level = SurfaceLevel.Strong,
        radius = Dim.rM,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(Dim.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dim.s3),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                Column(Modifier.weight(1f)) {
                    Text(
                        current?.name ?: "Нет доступного источника",
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        if (current == null) "все источники недоступны"
                        else "${current.lang} · до гл. ${current.maxChapter}" +
                            if (pinned) " · закреплён" else "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onChange)
                        .padding(horizontal = Dim.s3, vertical = 6.dp)
                ) { Text("Источник", fontSize = 12.sp) }
            }
            if (current != null && lost > 0) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(a.warn.copy(alpha = 0.10f))
                        .padding(Dim.s3, Dim.s2)
                ) {
                    Text(
                        "Источник полнее недоступен — потеряно $lost гл. Повтор через 2 мин.",
                        fontSize = 11.sp, lineHeight = 15.sp, color = a.warn,
                    )
                }
            }
        }
    }
}
