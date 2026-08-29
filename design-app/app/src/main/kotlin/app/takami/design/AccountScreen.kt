package app.takami.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccountScreen(onBack: () -> Unit = {}) {
    var signup by rememberSaveable { mutableStateOf(false) }
    var mail by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(Dim.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹", fontSize = 24.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(horizontal = Dim.s3),
            )
            Text("Аккаунт", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            Modifier.fillMaxWidth().padding(Dim.s4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { Text("?", fontSize = 26.sp) }
            Text(
                "Аккаунт нужен только для синхронизации",
                fontSize = 12.sp, lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dim.s3),
            )
        }
        TabRow(
            selectedTabIndex = if (signup) 1 else 0,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(horizontal = Dim.s4),
        ) {
            Tab(selected = !signup, onClick = { signup = false }) {
                Text("Вход", fontSize = 13.sp, modifier = Modifier.padding(vertical = Dim.s3))
            }
            Tab(selected = signup, onClick = { signup = true }) {
                Text("Регистрация", fontSize = 13.sp, modifier = Modifier.padding(vertical = Dim.s3))
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(Dim.s4),
            verticalArrangement = Arrangement.spacedBy(Dim.s3),
        ) {
            OutlinedTextField(
                value = mail, onValueChange = { mail = it },
                label = { Text("Почта") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dim.rM),
            )
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dim.rM),
            )
            if (signup && pass.isNotEmpty()) {
                val strength = minOf(4, pass.length / 3)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier.weight(1f).height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i < strength) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
            Button(
                onClick = { busy = !busy },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(999.dp),
                enabled = mail.isNotBlank() && pass.length >= 4,
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (signup) "Создать аккаунт" else "Войти", fontSize = 14.sp)
                }
            }
            Text(
                "Продолжить без аккаунта",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
                    .clickable(onClick = onBack)
                    .padding(vertical = Dim.s3),
            )
        }
    }
}

@Preview(name = "Account", showBackground = true, heightDp = 800)
@Composable
private fun PreviewAcc() = TakamiTheme(dark = true) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) { AccountScreen() }
}
