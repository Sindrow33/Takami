package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryScreen(onOpen: (FakeTitle) -> Unit = {}) {
    var filter by remember { mutableStateOf<Fmt?>(null) }
    val list = Fake.titles.filter { filter == null || it.fmt == filter }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Библиотека", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(Dim.s4, Dim.s3),
        )
        Row(
            Modifier.fillMaxWidth().padding(Dim.s4, 0.dp, Dim.s4, Dim.s3),
            horizontalArrangement = Arrangement.spacedBy(Dim.s2),
        ) {
            Chip("Всё", filter == null) { filter = null }
            Fmt.entries.forEach { f -> Chip(f.title, filter == f) { filter = f } }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            contentPadding = PaddingValues(Dim.s4, 0.dp, Dim.s4, 96.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.s3),
            verticalArrangement = Arrangement.spacedBy(Dim.s3),
        ) {
            items(list, key = { it.id }) { t -> TitleCard(t) { onOpen(t) } }
        }
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (on) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dim.s3, vertical = 6.dp)
    ) {
        Text(
            text, fontSize = 13.sp,
            color = if (on) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Library", showBackground = true, heightDp = 800)
@Composable
private fun PreviewLib() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { LibraryScreen() }
}
