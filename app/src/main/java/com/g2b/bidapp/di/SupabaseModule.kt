package com.g2b.bidapp.di

import com.g2b.bidapp.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.engine.android.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "com.g2b.bidapp"
                host = "login-callback"
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
            install(Postgrest)
            install(Realtime)
            install(Functions) // 향후 나라장터 상태 스케줄러 기능 호출 시 주석 해제
            httpEngine = Android.create()
        }

    @Provides
    @Singleton
    fun provideAuth(client: SupabaseClient): Auth = client.auth

    @Provides
    @Singleton
    fun providePostgrest(client: SupabaseClient): Postgrest = client.postgrest

    @Provides
    @Singleton
    fun provideRealtime(client: SupabaseClient): Realtime = client.realtime

    @Provides
    @Singleton
    fun provideFunctions(client: SupabaseClient): Functions = client.functions

    @Provides
    @Singleton
    fun provideAuthRedirectFlow(): MutableSharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 1)

    // FCM 알림 탭 → 공고 상세 화면 이동용
    // StateFlow 사용: 콜드 스타트 시 AppNavGraph collect 전 emit 유실 방지
    // null = 대기 중인 딥링크 없음, non-null = 이동할 bidNtceNo
    @Provides
    @Singleton
    @Named("fcmDeepLink")
    fun provideFcmDeepLinkFlow(): MutableStateFlow<String?> =
        MutableStateFlow(null)
}