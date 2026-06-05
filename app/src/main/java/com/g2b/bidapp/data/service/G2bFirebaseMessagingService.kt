package com.g2b.bidapp.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.g2b.bidapp.MainActivity
import com.g2b.bidapp.R
import com.g2b.bidapp.data.local.dao.NotificationDao
import com.g2b.bidapp.data.local.entity.NotificationEntity
import com.g2b.bidapp.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class G2bFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationDao: NotificationDao
    @Inject
    lateinit var authRepository: AuthRepository
    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: return
        val body = message.data["body"] ?: return
        val bidNtceNo = message.data["bid_ntce_no"]
        val newStatus = message.data["new_status"]

        // Room에 저장
        CoroutineScope(Dispatchers.IO).launch {
            // 알림 설정 확인
            val prefs = dataStore.data.first()
            val shouldNotify = when (newStatus) {
                "CHANGED", "REOPENED"   -> prefs[KEY_NOTIFY_CHANGED] ?: true
                "CANCELLED"             -> prefs[KEY_NOTIFY_CANCELLED] ?: true
                "OPENED"                -> prefs[KEY_NOTIFY_OPENED] ?: true
                "FAILED_BID"            -> prefs[KEY_NOTIFY_OPENED] ?: true  // 개찰 결과(유찰)이므로 동일 설정 사용
                "AWARDED"               -> prefs[KEY_NOTIFY_OPENED] ?: true  // 개찰 결과(낙찰)이므로 동일 설정 사용
                "BID_CLOSED"            -> true // 마감은 항상 표시
                else                    -> true // 마감 임박, 투찰 마감은 항상 표시
            }
            if (!shouldNotify) return@launch

            notificationDao.insert(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    watchedBidId = message.data["watched_bid_id"],
                    bidNtceNm = title,
                    message = body,
                    receivedAt = System.currentTimeMillis(),
                )
            )
            showNotification(title, body, bidNtceNo)
        }
    }

    override fun onNewToken(token: String) {
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

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_BID_NTCE_NO, bidNtceNo)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            bidNtceNo.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // 탭 시 MainActivity 실행
            .build()

        notificationManager.notify(bidNtceNo.hashCode(), notification)
    }

    companion object {
        val KEY_NOTIFY_CHANGED = booleanPreferencesKey("notify_changed")
        val KEY_NOTIFY_CANCELLED = booleanPreferencesKey("notify_cancelled")
        val KEY_NOTIFY_OPENED = booleanPreferencesKey("notify_opened")
    }
}
