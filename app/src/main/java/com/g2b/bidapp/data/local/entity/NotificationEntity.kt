package com.g2b.bidapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val watchedBidId: String?,
    val bidNtceNm: String?,
    val message: String,
    val isRead: Boolean = false,
    val receivedAt: Long,
)
