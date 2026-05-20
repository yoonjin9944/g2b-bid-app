package com.g2b.bidapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bid_status_history",
    foreignKeys = [
        ForeignKey(
            entity = WatchedBidEntity::class,
            parentColumns = ["id"],
            childColumns = ["watched_bid_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["watched_bid_id"])],
)

data class BidStatusHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "watched_bid_id") val watchedBidId: String,
    @ColumnInfo(name = "previous_status") val previousStatus: String,
    @ColumnInfo(name = "new_status") val newStatus: String,
    @ColumnInfo(name = "detected_at") val detectedAt: Long,
)
