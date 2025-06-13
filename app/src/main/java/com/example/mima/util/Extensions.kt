package com.example.mima.util



private var lastClickTimeMap = mutableMapOf<String, Long>()

fun throttleClick(key: String = "default", intervalMillis: Long = 500, block: () -> Unit) {
    val currentTime = System.currentTimeMillis()
    val lastTime = lastClickTimeMap[key] ?: 0L

    if (currentTime - lastTime > intervalMillis) {
        lastClickTimeMap[key] = currentTime
        block()
    }
}
