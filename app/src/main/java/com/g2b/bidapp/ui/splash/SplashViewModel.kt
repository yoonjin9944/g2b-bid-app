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

    // 다운로드 완료, 설치 대기 중 (권한 요청 중이거나 설치 취소 후 재시도 가능 상태)
    data object ReadyToInstall : SplashUiState

    data class RecommendUpdate(
        val downloadUrl: String,
        val releaseNotes: String,
    ) : SplashUiState

    data object UpToDate : SplashUiState

    data class Error(val message: String) : SplashUiState

    // 버전 체크 자체 실패 (네트워크 오류 등) — 건너뛰기 버튼으로 계속 진행 가능
    data class VersionCheckError(val message: String) : SplashUiState

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

    // 버전 체크 실패 다이얼로그에서 "건너뛰기" 클릭 시
    fun onVersionCheckErrorDismissed() {
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
                    // 버전 체크 실패 시 에러를 화면에 표시 (조용히 넘어가지 않음)
                    android.util.Log.e("SplashVM", "버전 체크 실패: ${result.message}")
                    _uiState.value = SplashUiState.VersionCheckError(result.message)
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
                        // 다운로드 완료 → 즉시 ReadyToInstall 로 전환 (Downloading 에서 벗어남)
                        _uiState.value = SplashUiState.ReadyToInstall
                        when (val result = apkDownloader.installApk(state.file)) {
                            is InstallResult.Success -> Unit          // 설치 다이얼로그가 위에 뜸, 상태 유지
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
        val file = pendingInstallFile
        android.util.Log.d("SplashVM", "onResume() | pendingInstallFile=${file?.absolutePath} | canInstall=${apkDownloader.canInstall()}")
        if (file == null) {
            android.util.Log.d("SplashVM", "onResume() → pendingInstallFile null, skip")
            return
        }
        if (!apkDownloader.canInstall()) {
            android.util.Log.d("SplashVM", "onResume() → 권한 없음, ReadyToInstall 유지")
            _uiState.value = SplashUiState.ReadyToInstall
            return
        }
        android.util.Log.d("SplashVM", "onResume() → installApk 호출")
        when (val result = apkDownloader.installApk(file)) {
            is InstallResult.Success -> {
                android.util.Log.d("SplashVM", "onResume() → installApk Success")
                _uiState.value = SplashUiState.ReadyToInstall
            }
            is InstallResult.PermissionRequired -> {
                android.util.Log.d("SplashVM", "onResume() → installApk PermissionRequired (설정 화면 재오픈)")
                _uiState.value = SplashUiState.ReadyToInstall
            }
            is InstallResult.Failure -> {
                android.util.Log.e("SplashVM", "onResume() → installApk Failure: ${result.message}")
                pendingInstallFile = null
                _uiState.value = SplashUiState.Error(result.message)
            }
        }
    }

    // 설치하기 버튼 클릭 시 호출
    fun retryInstall() {
        val file = pendingInstallFile
        android.util.Log.d("SplashVM", "retryInstall() | pendingInstallFile=${file?.absolutePath} | canInstall=${apkDownloader.canInstall()}")
        if (file == null) return
        when (val result = apkDownloader.installApk(file)) {
            is InstallResult.Success -> {
                android.util.Log.d("SplashVM", "retryInstall() → Success")
                _uiState.value = SplashUiState.ReadyToInstall
            }
            is InstallResult.PermissionRequired -> {
                android.util.Log.d("SplashVM", "retryInstall() → PermissionRequired (설정 화면으로)")
                _uiState.value = SplashUiState.ReadyToInstall
            }
            is InstallResult.Failure -> {
                android.util.Log.e("SplashVM", "retryInstall() → Failure: ${result.message}")
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
