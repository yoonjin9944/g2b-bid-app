package com.g2b.bidapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.data.version.ApkDownloader
import com.g2b.bidapp.data.version.DownloadState
import com.g2b.bidapp.data.version.VersionCheckRepository
import com.g2b.bidapp.domain.model.VersionCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashUiState {
    data object Loading : SplashUiState

    data class ForceUpdate(
        val downloadUrl: String,
        val releaseNotes: String,
    ) : SplashUiState

    data class Downloading(val progress: Float) : SplashUiState

    data class RecommendUpdate(
        val downloadUrl: String,
        val releaseNotes: String,
    ) : SplashUiState

    data object UpToDate : SplashUiState

    data class Error(val message: String) : SplashUiState

    data object NavigateToLogin : SplashUiState

    data object NavigateToMain : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val versionCheckRepository: VersionCheckRepository,
    private val apkDownloader: ApkDownloader,
    private val auth: Auth
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        startVersionCheck()
    }

    fun onForceUpdateConfirmed(downloadUrl: String) {
        startDownload(downloadUrl)
    }

    fun onRecommendUpdateConfirmed(downloadUrl: String) {
        startDownload(downloadUrl)
    }

    fun onRecommendUpdateDismissed() {
        checkSession()
    }

    private fun startVersionCheck() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading
            when (val result = versionCheckRepository.checkVersion()) {
                is VersionCheckResult.ForceUpdate ->
                    _uiState.value = SplashUiState.ForceUpdate(
                        downloadUrl = result.downloadUrl,
                        releaseNotes = result.releaseNotes,
                    )

                is VersionCheckResult.RecommendUpdate ->
                    _uiState.value = SplashUiState.RecommendUpdate(
                        downloadUrl = result.downloadUrl,
                        releaseNotes = result.releaseNotes,
                    )

                is VersionCheckResult.UpToDate -> {
                    _uiState.value = SplashUiState.UpToDate
                    checkSession()
                }

                is VersionCheckResult.Error -> {
                    _uiState.value = SplashUiState.Error(result.message)
                    checkSession()
                }
            }
        }
    }

    private fun startDownload(downloadUrl: String) {
        viewModelScope.launch {
            apkDownloader.downloadApk(downloadUrl).collect { state ->
                when (state) {
                    is DownloadState.Progress ->
                        _uiState.value = SplashUiState.Downloading(state.fraction)

                    is DownloadState.Done ->
                        apkDownloader.installApk(state.file)

                    is DownloadState.Failure ->
                        _uiState.value = SplashUiState.Error(state.message)
                }
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            val session = try {
                auth.currentSessionOrNull()
            } catch (e: Exception) {
                null
            }
            _uiState.value = if (session != null) {
                SplashUiState.NavigateToMain
            } else {
                SplashUiState.NavigateToLogin
            }
        }
    }
}
