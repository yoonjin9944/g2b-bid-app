package com.g2b.bidapp.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.BuildConfig
import com.g2b.bidapp.di.IoDispatcher
import com.g2b.bidapp.domain.model.User
import com.g2b.bidapp.domain.repository.AuthRepository
import com.g2b.bidapp.domain.repository.WatchlistRepository
import com.g2b.bidapp.domain.usecase.SignInWithGoogleUseCase
import com.g2b.bidapp.domain.usecase.SignInWithKakaoUseCase
import com.g2b.bidapp.domain.usecase.UserCancelledSignInException
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithKakaoUseCase: SignInWithKakaoUseCase,
    private val authRepository: AuthRepository,
    private val watchlistRepository: WatchlistRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(activityContext: Context) {
        if (_uiState.value is LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading

        viewModelScope.launch(ioDispatcher) {
            val tokenResult = signInWithGoogleUseCase.getGoogleIdToken(
                activityContext = activityContext,
                webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            )

            if (tokenResult.isFailure) {
                val cause = tokenResult.exceptionOrNull()
                if (cause is UserCancelledSignInException) {
                    _uiState.value = LoginUiState.Idle
                } else {
                    Log.e(TAG, "Google ID 토큰 획득 실패", cause)
                    _uiState.value = LoginUiState.Error(cause?.message ?: "Google 로그인에 실패했습니다")
                }
                return@launch
            }

            val idToken = tokenResult.getOrThrow()
            val signInResult = signInWithGoogleUseCase.signIn(idToken)

            signInResult.fold(
                onSuccess = { user ->
                    registerFcmToken(userId = user.id)
                    watchlistRepository.syncWithSupabase()
                    _uiState.value = LoginUiState.Success(user)
                },
                onFailure = { e ->
                    Log.e(TAG, "Supabase 로그인 실패", e)
                    _uiState.value = LoginUiState.Error(e.message ?: "Supabase 인증에 실패했습니다")
                },
            )
        }
    }

    fun signInWithKakao() {
        if (_uiState.value is LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading

        viewModelScope.launch(ioDispatcher) {
            // [수정] 기존 세션 청소 및 수집 Job 취소 로직은 Repository 영역 내부로 양도되었으므로 제거

            // 1. 카카오 로그인 및 외부 브라우저 세션 콜백 주입 과정을 UseCase를 통해 실행
            val signInResult = signInWithKakaoUseCase()
            if (signInResult.isFailure) {
                val e = signInResult.exceptionOrNull()
                Log.e(TAG, "카카오 로그인 실패", e)
                _uiState.value = LoginUiState.Error(e?.message ?: "카카오 로그인 실패")
                return@launch
            }

            Log.d(TAG, "카카오 인증 및 백엔드 동기화 완료. 최종 사용자 프로필 조회를 시도합니다.")

            // 2. Repository 내에서 이미 세션 확립 및 유저 데이터 갱신이 끝났으므로 곧바로 유저 정보 수집 가능
            val user = authRepository.getCurrentUser()

            if (user != null) {
                registerFcmToken(userId = user.id)
                watchlistRepository.syncWithSupabase()
                _uiState.value = LoginUiState.Success(user)
            } else {
                Log.e(TAG, "최종 유저 맵핑 실패 (인증 완료 후 유저 정보 누락)")
                _uiState.value = LoginUiState.Error("사용자 정보를 가져올 수 없습니다")
            }
        }
    }

    fun continueAsGuest() {
        _uiState.value = LoginUiState.Success(
            User(id = GUEST_USER_ID, email = "", displayName = "Guest")
        )
    }

    fun resetError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    private suspend fun registerFcmToken(userId: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            authRepository.upsertFcmToken(userId = userId, fcmToken = token)
        }.onFailure { e ->
            Log.w(TAG, "FCM 토큰 등록 실패 (로그인은 성공)", e)
        }
    }

    companion object {
        private const val TAG = "LoginViewModel"
        const val GUEST_USER_ID = "__guest__"
    }
}