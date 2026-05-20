package com.g2b.bidapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.g2b.bidapp.data.local.dao.BidStatusHistoryDao
import com.g2b.bidapp.data.local.dao.WatchedBidDao
import com.g2b.bidapp.data.local.entity.BidStatusHistoryEntity
import com.g2b.bidapp.data.local.entity.WatchedBidEntity

@Database(
    entities = [
        WatchedBidEntity::class,
        BidStatusHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)

abstract class G2bDatabase : RoomDatabase() {
    abstract fun watchedBidDao(): WatchedBidDao
    abstract fun bidStatusHistoryDao(): BidStatusHistoryDao
}