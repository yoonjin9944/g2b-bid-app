package com.g2b.bidapp.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val fcmToken: String? = null,
)
