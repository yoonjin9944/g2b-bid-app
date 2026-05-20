package com.g2b.bidapp.domain.usecase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.g2b.bidapp.domain.model.User
import com.g2b.bidapp.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context,
) {
    suspend fun getGoogleIdToken(
        activityContext: Context,
        webClientId: String,
    ): Result<String> = runCatching {
        val credentialManager = CredentialManager.create(activityContext)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)   // 이미 승인된 계정 필터
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)           // 자동 선택 비활성화 (보안)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            credentialManager.getCredential(
                request = request,
                context = activityContext,
            )
        } catch (e: GetCredentialException) {
            // 승인된 계정 없음 → filterByAuthorizedAccounts=false 로 재시도
            val fallbackOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackOption)
                .build()

            credentialManager.getCredential(
                request = fallbackRequest,
                context = activityContext,
            )
        }

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            googleCred.idToken
        } else {
            throw IllegalStateException("지원하지 않는 Credential 타입: ${credential.type}")
        }
    }.recoverWith<GetCredentialCancellationException, String> {
        // 사용자가 바텀시트를 닫은 경우 — 명시적 취소로 처리
        Result.failure(UserCancelledSignInException())
    }.recoverWith<GoogleIdTokenParsingException, String> {
        Result.failure(InvalidGoogleTokenException())
    }

    suspend fun signIn(idToken: String): Result<User> =
        authRepository.signInWithGoogleIdToken(idToken)
}

class UserCancelledSignInException : Exception("사용자가 로그인을 취소했습니다")
class InvalidGoogleTokenException : Exception("유효하지 않은 Google ID Token입니다")

private inline fun <reified E : Throwable, T> Result<T>.recoverWith(
    crossinline transform: (E) -> Result<T>,
): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { if (it is E) transform(it) else Result.failure(it) },
)