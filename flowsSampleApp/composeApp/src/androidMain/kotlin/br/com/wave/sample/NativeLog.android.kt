package br.com.wave.sample

import android.util.Log

actual fun logSdk(
    tag: String,
    message: String,
) {
    Log.d(tag, message)
}
