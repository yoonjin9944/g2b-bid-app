package com.g2b.bidapp.domain.repository

import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.WatchedBid
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun getWatchlistFlow(): Flow<List<WatchedBid>>
    fun getWatchlistByKeywordFlow(keyword: String): Flow<List<WatchedBid>>
    suspend fun getWatchedBidNos(): Set<String>
    suspend fun isWatched(bidNtceNo: String): Boolean
    suspend fun addToWatchlist(notice: BidNotice): Result<Unit>
    suspend fun removeFromWatchlist(bidNtceNo: String): Result<Unit>

    // 스와이프 삭제 실행취소 시 WatchedBid를 그대로 재삽입
    suspend fun restoreWatchedBid(bid: WatchedBid): Result<Unit>

    // 앱 시작 시 Supabase → Room 단방향 diff 동기화
    suspend fun syncWithSupabase(): Result<Unit>
}