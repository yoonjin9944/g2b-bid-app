package com.g2b.bidapp.data.repository

import com.g2b.bidapp.data.local.dao.WatchedBidDao
import com.g2b.bidapp.data.mapper.toModel
import com.g2b.bidapp.data.mapper.toSupabaseBidNotice
import com.g2b.bidapp.data.mapper.toWatchedBidEntity
import com.g2b.bidapp.data.supabase.dto.SupabaseBidNotice
import com.g2b.bidapp.di.IoDispatcher
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.domain.repository.WatchlistRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchedBidDao: WatchedBidDao,
    private val auth: Auth,
    private val postgrest: Postgrest,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WatchlistRepository {

    override fun getWatchlistFlow(): Flow<List<WatchedBid>> =
        watchedBidDao.getAllFlow().map { entities -> entities.map { it.toModel() } }

    override fun getWatchlistByKeywordFlow(keyword: String): Flow<List<WatchedBid>> =
        watchedBidDao.getByKeywordFlow(keyword).map { entities -> entities.map { it.toModel() } }

    override suspend fun getWatchedBidNos(): Set<String> =
        withContext(ioDispatcher) { watchedBidDao.getAllBidNtceNos().toSet() }

    override suspend fun isWatched(bidNtceNo: String): Boolean =
        withContext(ioDispatcher) { watchedBidDao.getByBidNtceNo(bidNtceNo) != null }

    // 등록: Room INSERT 선행 → Supabase upsert
    override suspend fun addToWatchlist(notice: BidNotice): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val entity = notice.toWatchedBidEntity()
            watchedBidDao.insertOrIgnore(entity)

            val userId = auth.currentUserOrNull()?.id ?: return@withContext
            runCatching {
                postgrest.from("bid_notices")
                    .upsert(notice.toSupabaseBidNotice(userId)) {
                        onConflict = "user_id,bid_ntce_no"
                        ignoreDuplicates = false
                    }
                watchedBidDao.updateSyncedAt(entity.id, System.currentTimeMillis())
            }
            // Supabase 실패 시 Room 데이터 유지(syncedAt = null). syncWithSupabase()가 재시도함
        }
    }

    // 삭제: Supabase DELETE → Room DELETE
    override suspend fun removeFromWatchlist(bidNtceNo: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val userId = auth.currentUserOrNull()?.id
            if (userId != null) {
                runCatching {
                    postgrest.from("bid_notices").delete {
                        filter {
                            eq("user_id", userId)
                            eq("bid_ntce_no", bidNtceNo)
                        }
                    }
                }
            }
            // Supabase 결과 무관하게 Room에서 삭제 (SSOT: Room)
            watchedBidDao.deleteByBidNtceNo(bidNtceNo)
        }
    }

    // 앱 시작 시: Supabase 전체 조회 → Room diff → 누락분 INSERT
    override suspend fun syncWithSupabase(): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val userId = auth.currentUserOrNull()?.id ?: return@withContext

            val remoteList = postgrest.from("bid_notices")
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseBidNotice>()

            val localNos = watchedBidDao.getAllBidNtceNos().toSet()

            remoteList
                .filter { it.bidNtceNo !in localNos }
                .forEach { dto -> watchedBidDao.insertOrIgnore(dto.toWatchedBidEntity()) }

            // TODO: Phase 8 — Room-only(syncedAt = null) 항목을 WorkManager 재시도 큐에 등록
        }
    }
}
