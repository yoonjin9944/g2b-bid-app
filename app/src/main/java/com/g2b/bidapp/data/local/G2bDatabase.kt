package com.g2b.bidapp.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.g2b.bidapp.data.local.dao.BidStatusHistoryDao
import com.g2b.bidapp.data.local.dao.NotificationDao
import com.g2b.bidapp.data.local.dao.WatchedBidDao
import com.g2b.bidapp.data.local.entity.BidStatusHistoryEntity
import com.g2b.bidapp.data.local.entity.NotificationEntity
import com.g2b.bidapp.data.local.entity.WatchedBidEntity

@Database(
    entities = [
        WatchedBidEntity::class,
        BidStatusHistoryEntity::class,
        NotificationEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ]
)

abstract class G2bDatabase : RoomDatabase() {
    abstract fun watchedBidDao(): WatchedBidDao
    abstract fun bidStatusHistoryDao(): BidStatusHistoryDao
    abstract fun notificationDao(): NotificationDao
}