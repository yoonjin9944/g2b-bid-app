package com.g2b.bidapp.data.service

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.g2b.bidapp.data.local.dao.WatchedBidDao
import com.g2b.bidapp.domain.model.BidStatus
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SupabaseRealtimeService : LifecycleService() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    @Inject
    lateinit var watchedBidDao: WatchedBidDao

    private var realtimeJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startRealtimeSubscription()
        return START_STICKY
    }

    private fun startRealtimeSubscription() {
        realtimeJob = lifecycleScope.launch {
            supabaseClient.realtime.connect()
            val channel = supabaseClient.realtime.channel("bid_notices_changes")

            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "bid_notices"
            }.collect { update ->
                val bidNtceNo = update.record["bid_ntce_no"]?.toString() ?: return@collect
                val newStatus = update.record["current_status"]?.toString() ?: return@collect
                val bidStatus = BidStatus.entries.find { it.name == newStatus } ?: return@collect
                watchedBidDao.updateStatus(bidNtceNo, bidStatus.name)
            }

            channel.subscribe()
        }
    }

    override fun onDestroy() {
        realtimeJob?.cancel()
        lifecycleScope.launch {
            runCatching { supabaseClient.realtime.disconnect() }
        }
        super.onDestroy()
    }

}