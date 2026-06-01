package com.g2b.bidapp.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ]
)
abstract class G2bDatabase : RoomDatabase() {
    abstract fun watchedBidDao(): WatchedBidDao
    abstract fun bidStatusHistoryDao(): BidStatusHistoryDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watched_bids ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP INDEX IF EXISTS `index_watched_bids_bid_ntce_no`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_watched_bids_user_id_bid_ntce_no` ON `watched_bids` (`user_id`, `bid_ntce_no`)")
            }
        }
    }
}
