package com.g2b.bidapp.data.repository

import com.g2b.bidapp.data.supabase.dto.SupabaseUser
import com.g2b.bidapp.domain.model.User
import com.g2b.bidapp.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
) : AuthRepository {
    override suspend fun signInWithGoogleIdToken(idToken: String): Result<User> = runCatching {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
        }

        val session = auth.currentSessionOrNull()
            ?: error("Supabase 세션 생성에 실패했습니다")

        val supabaseUser = auth.currentUserOrNull()
            ?: error("Supabase 사용자 정보를 가져올 수 없습니다")

        val userRow = SupabaseUser(
            id = supabaseUser.id,
            email = supabaseUser.email ?: "",
            displayName = supabaseUser.userMetadata
                ?.get("full_name")
                ?.toString()
                ?.trim('"'),
        )

        runCatching {
            postgrest.from("users")
                .upsert(userRow) {
                    onConflict = "id"
                    ignoreDuplicates = false
                }
        }

        User(
            id = supabaseUser.id,
            email = supabaseUser.email ?: "",
            displayName = userRow.displayName,
        )
    }


    // 현재 세션
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

    // 마지막 로그인 기기의 토큰만 유지
    override suspend fun upsertFcmToken(userId: String, fcmToken: String): Result<Unit> = runCatching {
        postgrest.from("users")
            .update(mapOf("fcm_token" to fcmToken)) {
                filter {
                    eq("id", userId)
                }
            }
    }
}