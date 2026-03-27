package br.com.wave.sample

import androidx.compose.runtime.Composable

@Composable
expect fun PasswordVisibilityToggle(
    passwordVisible: Boolean,
    onToggle: () -> Unit,
)
