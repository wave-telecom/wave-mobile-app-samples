package br.com.wave.sample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform