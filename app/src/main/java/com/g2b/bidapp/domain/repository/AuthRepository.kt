package com.g2b.bidapp.domain.repository

import com.g2b.bidapp.domain.model.User

interface AuthRepository {
    // Google ID Token -> Supabase 인증
    suspend fun signInWithGoogleIdToken(idToken: String): Result<User>

    // Supabase session 유효성 확인
    suspend fun getCurrentUser(): User?

    // 세션 파기
    suspend fun signOut(): Result<Unit>

    // 로그인 후 FCM 토큰 Upsert(merge)
    suspend fun upsertFcmToken(userId: String, fcmToken: String): Result<Unit>

    suspend fun signInWithKakao(): Result<Unit>
}