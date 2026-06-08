package com.g2b.bidapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.data.version.ApkDownloader
import com.g2b.bidapp.data.version.ApkDownloader.InstallResult
import com.g2b.bidapp.data.version.DownloadState
import com.g2b.bidapp.data.version.VersionCheckRepository
import com.g2b.bidapp.domain.model.VersionCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
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

    // FCM 딥링크로 진입 시 — 로그인 상태 + 이동할 bidNtceNo 보유
    data class NavigateToDetail(val bidNtceNo: String) : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val versionCheckRepository: VersionCheckRepository,
    private val apkDownloader: ApkDownloader,
    private val auth: Auth,
    @javax.inject.Named("fcmDeepLink")
    private val fcmDeepLinkFlow: MutableStateFlow<String?>,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    // 설정 화면에서 돌아왔을 때 재시도할 파일 보관
    private var pendingInstallFile: File? = null

    // 다운로드 중복 실행 방지
    private var isDownloading = false

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
        if (isDownloading) return  // 중복 실행 방지
        isDownloading = true
        _uiState.value = SplashUiState.Downloading(0f)  // 즉시 상태 전환 → 버튼 중복 클릭 차단

        viewModelScope.launch {
            apkDownloader.downloadApk(downloadUrl).collect { state ->
                when (state) {
                    is DownloadState.Progress ->
                        _uiState.value = SplashUiState.Downloading(state.fraction)

                    is DownloadState.Done -> {
                        pendingInstallFile = state.file
                        isDownloading = false
                        when (val result = apkDownloader.installApk(state.file)) {
                            is InstallResult.Success -> Unit          // 설치 화면으로 넘어감
                            is InstallResult.PermissionRequired -> Unit // 설정 화면으로 이동 — onResume 에서 재시도
                            is InstallResult.Failure -> {
                                pendingInstallFile = null
                                _uiState.value = SplashUiState.Error(result.message)
                            }
                        }
                    }

                    is DownloadState.Failure -> {
                        _uiState.value = SplashUiState.Error(state.message)
                        isDownloading = false
                    }
                }
            }
        }
    }

    // SplashScreen 이 Resume 될 때 호출 — 설정 화면에서 허용 후 돌아온 경우 설치 재시도
    fun onResume() {
        val file = pendingInstallFile ?: return
        if (!apkDownloader.canInstall()) return

        when (val result = apkDownloader.installApk(file)) {
            is InstallResult.Success -> pendingInstallFile = null
            is InstallResult.PermissionRequired -> Unit  // 아직 권한 없음 — 대기
            is InstallResult.Failure -> {
                pendingInstallFile = null
                _uiState.value = SplashUiState.Error(result.message)
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
                // FCM 딥링크가 대기 중이면 BidDetail로 바로 이동
                val pendingBidNtceNo = fcmDeepLinkFlow.value
                if (pendingBidNtceNo != null) {
                    fcmDeepLinkFlow.value = null  // 소비
                    SplashUiState.NavigateToDetail(pendingBidNtceNo)
                } else {
                    SplashUiState.NavigateToMain
                }
            } else {
                SplashUiState.NavigateToLogin
            }
        }
    }
}
