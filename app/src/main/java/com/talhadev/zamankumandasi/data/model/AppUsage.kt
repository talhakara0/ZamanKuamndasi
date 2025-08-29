package com.talhadev.zamankumandasi.data.model

data class AppUsage(
    val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val dailyLimit: Long = 0, // milisaniye cinsinden
    val usedTime: Long = 0, // milisaniye cinsinden
    val lastUsed: Long = 0,
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: String? = null
)
