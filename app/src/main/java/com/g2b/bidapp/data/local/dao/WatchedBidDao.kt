package com.g2b.bidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.g2b.bidapp.data.local.entity.WatchedBidEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedBidDao {

    @Query("SELECT * FROM watched_bids WHERE user_id = :userId ORDER BY watched_at DESC")
    fun getAllFlow(userId: String): Flow<List<WatchedBidEntity>>

    @Query("SELECT * FROM watched_bids WHERE user_id = :userId AND bid_ntce_nm LIKE '%' || :keyword || '%' ORDER BY watched_at DESC")
    fun getByKeywordFlow(userId: String, keyword: String): Flow<List<WatchedBidEntity>>

    @Query("SELECT bid_ntce_no FROM watched_bids WHERE user_id = :userId")
    suspend fun getAllBidNtceNos(userId: String): List<String>

    @Query("SELECT * FROM watched_bids WHERE user_id = :userId AND bid_ntce_no = :bidNtceNo LIMIT 1")
    suspend fun getByBidNtceNo(userId: String, bidNtceNo: String): WatchedBidEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: WatchedBidEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchedBidEntity): Long

    @Query("DELETE FROM watched_bids WHERE user_id = :userId AND bid_ntce_no = :bidNtceNo")
    suspend fun deleteByBidNtceNo(userId: String, bidNtceNo: String)

    @Query("DELETE FROM watched_bids WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM watched_bids WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("UPDATE watched_bids SET synced_at = :syncedAt WHERE id = :id")
    suspend fun updateSyncedAt(id: String, syncedAt: Long)

    @Query("UPDATE watched_bids SET current_status = :newStatus, synced_at = :syncedAt WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String, syncedAt: Long)

    // Supabase Realtime 수신 시 사용 — Supabase RLS가 사용자별 이벤트를 보장하므로 userId 필터 생략
    @Query("UPDATE watched_bids SET current_status = :status WHERE bid_ntce_no = :bidNtceNo")
    suspend fun updateStatus(bidNtceNo: String, status: String)

    @Query("SELECT * FROM watched_bids WHERE user_id = :userId AND synced_at IS NULL")
    suspend fun getUnsynced(userId: String): List<WatchedBidEntity>
}
