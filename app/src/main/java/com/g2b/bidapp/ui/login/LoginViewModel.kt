package com.g2b.bidapp.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.BuildConfig
import com.g2b.bidapp.di.IoDispatcher
import com.g2b.bidapp.domain.model.User
import com.g2b.bidapp.domain.repository.AuthRepository
import com.g2b.bidapp.domain.usecase.SignInWithGoogleUseCase
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
    private val authRepository: AuthRepository,
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
                    _uiState.value = LoginUiState.Success(user)
                },
                onFailure = { e ->
                    Log.e(TAG, "Supabase 로그인 실패", e)
                    _uiState.value = LoginUiState.Error(e.message ?: "Supabase 인증에 실패했습니다")
                },
            )
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