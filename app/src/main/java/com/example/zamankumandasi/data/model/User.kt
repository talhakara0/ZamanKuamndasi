package com.example.zamankumandasi.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val userType: UserType = UserType.CHILD,
    val parentId: String? = null,
    val pairingCode: String? = null,
    val deviceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserType {
    PARENT,
    CHILD
}
