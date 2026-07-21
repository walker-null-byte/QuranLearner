package com.dillu.quranlearner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform