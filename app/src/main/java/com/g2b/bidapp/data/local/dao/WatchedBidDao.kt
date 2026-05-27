package com.g2b.bidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.g2b.bidapp.data.local.entity.WatchedBidEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedBidDao {

    @Query("SELECT * FROM watched_bids ORDER BY watched_at DESC")
    fun getAllFlow(): Flow<List<WatchedBidEntity>>

    @Query("SELECT * FROM watched_bids WHERE bid_ntce_nm LIKE '%' || :keyword || '%' ORDER BY watched_at DESC")
    fun getByKeywordFlow(keyword: String): Flow<List<WatchedBidEntity>>

    @Query("SELECT bid_ntce_no FROM watched_bids")
    suspend fun getAllBidNtceNos(): List<String>

    @Query("SELECT * FROM watched_bids WHERE bid_ntce_no = :bidNtceNo LIMIT 1")
    suspend fun getByBidNtceNo(bidNtceNo: String): WatchedBidEntity?

    // PK 충돌 시 무시 (중복 등록 방지). 반환값: 삽입된 rowId, -1이면 무시됨
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: WatchedBidEntity): Long

    // Supabase UUID로 교체할 때 사용 (syncWithSupabase 시 기존 row 삭제 후 재삽입)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchedBidEntity): Long

    @Query("DELETE FROM watched_bids WHERE bid_ntce_no = :bidNtceNo")
    suspend fun deleteByBidNtceNo(bidNtceNo: String)

    @Query("DELETE FROM watched_bids WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE watched_bids SET synced_at = :syncedAt WHERE id = :id")
    suspend fun updateSyncedAt(id: String, syncedAt: Long)

    @Query("UPDATE watched_bids SET current_status = :newStatus, synced_at = :syncedAt WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String, syncedAt: Long)

    @Query("UPDATE watched_bids SET current_status = :status WHERE bid_ntce_no = :bidNtceNo")
    suspend fun updateStatus(bidNtceNo: String, status: String)

    @Query("SELECT * FROM watched_bids WHERE synced_at IS NULL")
    suspend fun getUnsynced(): List<WatchedBidEntity>
}