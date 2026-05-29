package com.g2b.bidapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.g2b.bidapp.data.local.dao.WatchedBidDao
import com.g2b.bidapp.data.mapper.toSupabaseBidNotice
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

@HiltWorker
class WatchlistSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val watchedBidDao: WatchedBidDao,
    private val auth: Auth,
    private val postgrest: Postgrest,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUserOrNull()?.id ?: return Result.failure()
        val unsynced = watchedBidDao.getUnsynced(userId)
        if (unsynced.isEmpty()) return Result.success()

        var allSuccess = true
        for (entity in unsynced) {
            runCatching {
                postgrest.from("bid_notices")
                    .upsert(entity.toSupabaseBidNotice(userId)) {
                        onConflict = "user_id,bid_ntce_no"
                        ignoreDuplicates = false
                    }
                watchedBidDao.updateSyncedAt(entity.id, System.currentTimeMillis())
            }.onFailure {
                allSuccess = false
            }
        }
        return if (allSuccess) Result.success() else Result.retry()
    }
}
