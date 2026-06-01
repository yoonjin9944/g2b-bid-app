package com.g2b.bidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.g2b.bidapp.data.local.entity.BidStatusHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BidStatusHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BidStatusHistoryEntity): Long

    @Query("SELECT * FROM bid_status_history WHERE watched_bid_id = :watchedBidId ORDER BY detected_at DESC")
    fun getByWatchedBidIdFlow(watchedBidId: String): Flow<List<BidStatusHistoryEntity>>
}