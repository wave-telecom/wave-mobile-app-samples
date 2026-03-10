package br.com.wave.sample

import platform.Foundation.NSLog

actual fun logSdk(
    tag: String,
    message: String,
) {
    NSLog("[$tag] $message")
}
