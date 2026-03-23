package br.com.wave.sample

import androidx.compose.runtime.Composable

@Composable
actual fun NativeBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
