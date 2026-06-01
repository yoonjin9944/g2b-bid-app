package com.g2b.bidapp.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.data.local.dao.NotificationDao
import com.g2b.bidapp.data.local.entity.NotificationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationListViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> = notificationDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun markRead(id: String) {
        viewModelScope.launch { notificationDao.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notificationDao.markAllRead() }
    }
}
