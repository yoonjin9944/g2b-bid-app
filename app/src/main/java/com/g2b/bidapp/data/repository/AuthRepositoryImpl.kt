package com.g2b.bidapp.data.repository

import android.util.Log
import com.g2b.bidapp.domain.model.User
import com.g2b.bidapp.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.parseSessionFromUrl
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val authRedirectFlow: MutableSharedFlow<String>,
) : AuthRepository {

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<User> = runCatching {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
        }

        val supabaseUser = auth.currentUserOrNull()
            ?: error("Supabase 사용자 정보를 가져올 수 없습니다")

        // [수정] 클라이언트 측 postgrest.from("users").upsert 로직을 완전히 제거했습니다.
        // 데이터베이스의 AFTER INSERT 트리거가 public.users 테이블 적재를 자동으로 처리합니다.

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
        auth.signOut()
    }

    // 마지막 로그인 기기의 FCM 토큰 업데이트 (통합 테이블 public.users의 컬럼을 업데이트)
    override suspend fun upsertFcmToken(userId: String, fcmToken: String): Result<Unit> = runCatching {
        postgrest.from("users")
            .update(mapOf("fcm_token" to fcmToken)) {
                filter {
                    eq("id", userId)
                }
            }
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

                Unit
            } else {
                throw IllegalStateException("수신된 딥링크 URL에서 세션을 파싱하지 못했습니다.")
            }
        }
    }.onFailure { e ->
        Log.e("AuthRepository", "카카오 로그인 최종 실패 단계 예외 발생", e)
    }


}