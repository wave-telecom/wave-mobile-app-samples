package br.com.wave.sample

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
actual fun PasswordVisibilityToggle(
    passwordVisible: Boolean,
    onToggle: () -> Unit,
) {
    TextButton(onClick = onToggle) {
        Text(if (passwordVisible) "Hide" else "Show")
    }
}
