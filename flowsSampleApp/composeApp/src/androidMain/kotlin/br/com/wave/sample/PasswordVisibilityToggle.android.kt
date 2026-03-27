package br.com.wave.sample

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
actual fun PasswordVisibilityToggle(
    passwordVisible: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector =
                if (passwordVisible) {
                    Icons.Filled.VisibilityOff
                } else {
                    Icons.Filled.Visibility
                },
            contentDescription =
                if (passwordVisible) {
                    "Ocultar senha"
                } else {
                    "Mostrar senha"
                },
        )
    }
}
