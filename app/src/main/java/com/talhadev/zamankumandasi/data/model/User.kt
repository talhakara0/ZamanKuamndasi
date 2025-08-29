package com.talhadev.zamankumandasi.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val userType: UserType = UserType.CHILD,
    val parentId: String? = null,
    val pairingCode: String? = null,
    // Premium hesaplar: reklam yok, ek özellikler açık
    val isPremium: Boolean = false,
    val deviceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserType {
    PARENT,
    CHILD
}
