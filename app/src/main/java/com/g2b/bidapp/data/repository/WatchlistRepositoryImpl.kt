package com.g2b.bidapp.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.g2b.bidapp.data.local.dao.WatchedBidDao
import com.g2b.bidapp.data.mapper.toEntity
import com.g2b.bidapp.data.mapper.toModel
import com.g2b.bidapp.data.mapper.toSupabaseBidNotice
import com.g2b.bidapp.data.mapper.toWatchedBidEntity
import com.g2b.bidapp.data.supabase.dto.SupabaseBidNotice
import com.g2b.bidapp.data.worker.WatchlistSyncWorker
import com.g2b.bidapp.di.IoDispatcher
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.domain.repository.WatchlistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val watchedBidDao: WatchedBidDao,
    private val auth: Auth,
    private val postgrest: Postgrest,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WatchlistRepository {

    private fun currentUserId(): String = auth.currentUserOrNull()?.id ?: ""

    override fun getWatchlistFlow(): Flow<List<WatchedBid>> =
        watchedBidDao.getAllFlow(currentUserId()).map { entities -> entities.map { it.toModel() } }

    override fun getWatchlistByKeywordFlow(keyword: String): Flow<List<WatchedBid>> =
        watchedBidDao.getByKeywordFlow(currentUserId(), keyword).map { entities -> entities.map { it.toModel() } }

    override suspend fun getWatchedBidNos(): Set<String> =
        withContext(ioDispatcher) { watchedBidDao.getAllBidNtceNos(currentUserId()).toSet() }

    override suspend fun isWatched(bidNtceNo: String): Boolean =
        withContext(ioDispatcher) { watchedBidDao.getByBidNtceNo(currentUserId(), bidNtceNo) != null }

    // 등록: Room INSERT 선행 → Supabase upsert
    override suspend fun addToWatchlist(notice: BidNotice): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val userId = auth.currentUserOrNull()?.id ?: error("로그인이 필요합니다")
            val entity = notice.toWatchedBidEntity(userId)
            watchedBidDao.insertOrIgnore(entity)

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
            watchedBidDao.deleteByBidNtceNo(userId ?: "", bidNtceNo)
        }
    }

    // Snackbar 실행취소 시 WatchedBid를 Room에 재삽입 + Supabase upsert 시도
    override suspend fun restoreWatchedBid(bid: WatchedBid): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val userId = auth.currentUserOrNull()?.id ?: error("로그인이 필요합니다")
            val entity = bid.toEntity(userId)
            watchedBidDao.insertOrIgnore(entity)

            runCatching {
                val supabaseDto = entity.toSupabaseBidNotice(userId)
                postgrest.from("bid_notices")
                    .upsert(supabaseDto) {
                        onConflict = "user_id,bid_ntce_no"
                        ignoreDuplicates = false
                    }
                watchedBidDao.updateSyncedAt(entity.id, System.currentTimeMillis())
            }
        }
    }

    // 앱 시작 시: Supabase 전체 조회 → Room diff → 누락분 INSERT
    override suspend fun syncWithSupabase(): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val userId = auth.currentUserOrNull()?.id ?: return@withContext

            val remoteList = postgrest.from("bid_notices")
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseBidNotice>()

            val localNos = watchedBidDao.getAllBidNtceNos(userId).toSet()

            remoteList
                .filter { it.bidNtceNo !in localNos }
                .forEach { dto -> watchedBidDao.insertOrIgnore(dto.toWatchedBidEntity()) }

            // Room-only(syncedAt = null) 항목을 WorkManager 재시도 큐에 등록
            val hasUnsynced = watchedBidDao.getUnsynced(userId).isNotEmpty()
            if (hasUnsynced) {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "watchlist_sync",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<WatchlistSyncWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                        .build()
                )
            }
        }
    }

    // 로그아웃 시 현재 사용자의 로컬 데이터 삭제
    override suspend fun clearLocalData(): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val userId = auth.currentUserOrNull()?.id ?: return@withContext
            watchedBidDao.deleteAllByUserId(userId)
        }
    }
}
