package com.vishnurajeevan.libroabs

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform