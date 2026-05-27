package com.g2b.bidapp.ui.settings

import android.annotation.SuppressLint
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.domain.model.User
import com.g2b.bidapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettings(
    val notifyChanged: Boolean = true,
    val notifyCancelled: Boolean = true,
    val notifyOpened: Boolean = true,
)

data class SettingsUiState(
    val isLoggedOut: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
open class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val notificationSettings: StateFlow<NotificationSettings> = dataStore.data
        .map { prefs ->
            NotificationSettings(
                notifyChanged = prefs[KEY_NOTIFY_CHANGED] ?: true,
                notifyCancelled = prefs[KEY_NOTIFY_CANCELLED] ?: true,
                notifyOpened = prefs[KEY_NOTIFY_OPENED] ?: true,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettings(),
        )

    fun setNotifyChanged(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[KEY_NOTIFY_CHANGED] = enabled } }
    }

    fun setNotifyCancelled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[KEY_NOTIFY_CANCELLED] = enabled } }
    }

    fun setNotifyOpened(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[KEY_NOTIFY_OPENED] = enabled } }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.signOut()
            _uiState.update { it.copy(isLoggedOut = true, isLoading = false) }
        }
    }

    companion object {
        val KEY_NOTIFY_CHANGED = booleanPreferencesKey("notify_changed")
        val KEY_NOTIFY_CANCELLED = booleanPreferencesKey("notify_cancelled")
        val KEY_NOTIFY_OPENED = booleanPreferencesKey("notify_opened")

        fun preview() = @SuppressLint("StaticFieldLeak")
        object : SettingsViewModel(
            authRepository = object : AuthRepository {
                override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success(User("", "", null))
                override suspend fun getCurrentUser() = null
                override suspend fun signOut() = Result.success(Unit)
                override suspend fun upsertFcmToken(userId: String, fcmToken: String) = Result.success(Unit)
                override suspend fun signInWithKakao() = Result.success(Unit)
            },
            dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
                produceFile = { java.io.File(System.getProperty("java.io.tmpdir"), "preview_settings.preferences_pb") }
            ),
        ) {}
    }
}
