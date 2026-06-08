package com.g2b.bidapp.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.g2b.bidapp.data.service.G2bFirebaseMessagingService.Companion.KEY_PENDING_FCM_TOKEN
import com.g2b.bidapp.domain.model.User
import com.g2b.bidapp.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.parseSessionFromUrl
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val authRedirectFlow: MutableSharedFlow<String>,
    private val dataStore: DataStore<Preferences>,
) : AuthRepository {

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<User> = runCatching {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
        }

//        val supabaseUser = auth.currentUserOrNull()
//            ?: error("Supabase 사용자 정보를 가져올 수 없습니다")
        val supabaseUser = auth.retrieveUserForCurrentSession(updateSession = true)
        Log.d("AuthRepository", "서버 유저 정보 동기화 완벽 완료: ${supabaseUser.id}")

        // [수정] 클라이언트 측 postgrest.from("users").upsert 로직을 완전히 제거했습니다.
        // 데이터베이스의 AFTER INSERT 트리거가 public.users 테이블 적재를 자동으로 처리합니다.

        try {
            // DataStore에 임시 저장된 토큰 우선 사용, 없으면 Firebase에서 직접 조회
            val token = dataStore.data.first()[KEY_PENDING_FCM_TOKEN]
                ?: FirebaseMessaging.getInstance().token.await()
            upsertFcmToken(supabaseUser.id, token)
        } catch (e: Exception) {
            Log.e("AuthRepository", "upsertFcmToken 실패", e)
        }

        User(
            id = supabaseUser.id,
            email = supabaseUser.email ?: "",
            displayName = supabaseUser.userMetadata
                ?.get("full_name")
                ?.toString()
                ?.trim('"'),
        )
    }

    // 현재 세션 확인 및 도메인 모델 매핑
    override suspend fun getCurrentUser(): User? {
        val supabaseUser = auth.currentUserOrNull() ?: return null
        return User(
            id = supabaseUser.id,
            email = supabaseUser.email ?: "",
            displayName = supabaseUser.userMetadata
                ?.get("full_name")
                ?.toString()
                ?.trim('"'),
        )
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        try {
            val userId = auth.currentUserOrNull()?.id
            val token = FirebaseMessaging.getInstance().token.await()
            if (userId != null) {
                postgrest.from("user_fcm_tokens").delete {
                    filter {
                        eq("user_id", userId)
                        eq("fcm_token", token)
                    }
                }
            }
        } catch (_: Exception) {}
        auth.signOut()
    }

    override suspend fun upsertFcmToken(userId: String, fcmToken: String): Result<Unit> = runCatching {
        // fcm_token UNIQUE 기준으로 upsert
        // → 동일 기기에서 계정 전환 시 user_id가 현재 계정으로 덮어씌워짐
        postgrest.from("user_fcm_tokens")
            .upsert(mapOf("user_id" to userId, "fcm_token" to fcmToken)) {
                onConflict = "fcm_token"
                ignoreDuplicates = false
            }
        Log.d("upsertFcmToken", "upsertFcmToken 호출 | userId=$userId | token=${fcmToken.take(20)}")
    }

    override suspend fun signInWithKakao(): Result<Unit> = runCatching {
        Log.d("AuthRepository", "카카오 로그인 프로세스 시작")

        // 주입구 규칙에 맞게 redirectTo 주소 뒤에 카카오 강제 동의 파라미터를 결합합니다.
        auth.signInWith(Kakao)

        withTimeout(60_000L.milliseconds) {
            Log.d("AuthRepository", "authRedirectFlow 수집 대기 시작...")
            val redirectUrl = authRedirectFlow.first()
            Log.d("AuthRepository", "리다이렉트 URL 수신 성공! -> 세션 주입 시작")
            Log.d("AuthRepository", "redirectUrl = $redirectUrl")

            val session = auth.parseSessionFromUrl(redirectUrl)
            Log.d("AuthRepository", "parsed session = $session")
            if (session != null) {
                auth.importSession(session)
                Log.d("AuthRepository", "Supabase 세션 주입 완료. 서버 유저 정보 동기화 시작.")

                auth.importSession(session)
                Log.d(
                    "AuthRepository",
                    "세션 import 완료"
                )

                auth.sessionStatus.first {
                    it is SessionStatus.Authenticated
                }

                Log.d(
                    "AuthRepository",
                    "Authenticated 상태 확인 완료"
                )

                Log.d(
                    "AuthRepository",
                    "import 후 currentSession = ${auth.currentSessionOrNull()}"
                )

                Log.d(
                    "AuthRepository",
                    "import 후 currentUser = ${auth.currentUserOrNull()}"
                )

                val supabaseUser = auth.retrieveUserForCurrentSession(updateSession = true)
                Log.d("AuthRepository", "서버 유저 정보 동기화 완벽 완료: ${supabaseUser.id}")

                try {
                    // DataStore에 임시 저장된 토큰 우선 사용, 없으면 Firebase에서 직접 조회
                    val token = dataStore.data.first()[KEY_PENDING_FCM_TOKEN]
                        ?: FirebaseMessaging.getInstance().token.await()
                    upsertFcmToken(supabaseUser.id, token)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "upsertFcmToken 실패 (카카오)", e)
                }

                Unit
            } else {
                throw IllegalStateException("수신된 딥링크 URL에서 세션을 파싱하지 못했습니다.")
            }
        }
    }.onFailure { e ->
        Log.e("AuthRepository", "카카오 로그인 최종 실패 단계 예외 발생", e)
    }


}