package com.g2b.bidapp.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.g2b.bidapp.R
import com.g2b.bidapp.data.local.dao.NotificationDao
import com.g2b.bidapp.data.local.entity.NotificationEntity
import com.g2b.bidapp.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class G2bFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationDao: NotificationDao
    @Inject lateinit var authRepository: AuthRepository

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: return
        val body = message.data["body"] ?: return
        val bidNtceNo = message.data["bid_ntce_no"]
        val newStatus = message.data["new_status"]

        // Room에 저장
        CoroutineScope(Dispatchers.IO).launch {
            notificationDao.insert(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    watchedBidId = message.data["watched_bid_id"],
                    bidNtceNm = title,
                    message = body,
                    receivedAt = System.currentTimeMillis(),
                )
            )
        }

        // 시스템 알림 표시
        showNotification(title, body, bidNtceNo)
    }

    override fun onNewToken(token: String) {
        // FCM 토큰 갱신 시 Supabase users 테이블 업데이트
        CoroutineScope(Dispatchers.IO).launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            authRepository.upsertFcmToken(user.id, token)
        }
    }

    private fun showNotification(title: String, body: String, bidNtceNo: String?) {
        val channelId = "bid_status_updates"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "입찰 상태 변경",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(bidNtceNo.hashCode(), notification)
    }
}