package com.g2b.bidapp.di

import android.util.Log
import com.g2b.bidapp.BuildConfig
import com.g2b.bidapp.data.remote.api.BidPublicInfoApi
import com.g2b.bidapp.data.remote.api.ScsbidInfoApi
import com.g2b.bidapp.data.remote.dto.BidItemsTypeAdapter
import com.g2b.bidapp.data.remote.dto.BidNoticeItems
import com.g2b.bidapp.data.remote.dto.BidResultItems
import com.g2b.bidapp.data.remote.dto.BidResultItemsTypeAdapter
import com.g2b.bidapp.data.remote.interceptor.AuthInterceptor
import com.g2b.bidapp.data.remote.interceptor.RetryInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.*
import javax.inject.Named
import javax.inject.Singleton

private const val HTTP_ERROR_CODE_MIN = 400
private const val HTTP_ERROR_CODE_MAX = 599

private val prettyGson = GsonBuilder().setPrettyPrinting().create()

private fun buildLoggingInterceptor(tag: String): HttpLoggingInterceptor =
    HttpLoggingInterceptor { message ->
        if (!BuildConfig.DEBUG) return@HttpLoggingInterceptor
        when {
            message.startsWith("-->") -> {
                Log.d(tag, "┌──────────────────────────── [$tag] ───")
                Log.d(tag, "│ ▶ ${message.removePrefix("--> ")}")
            }

            message.startsWith("--> END") -> Log.d(tag, "├─────────────────────────────────────────────")
            message.startsWith("<--") -> {
                val code = message.substringAfter("<--").take(3).toIntOrNull() ?: 0
                val logFn: (String, String) -> Unit =
                    if (code in HTTP_ERROR_CODE_MIN..HTTP_ERROR_CODE_MAX) Log::w else Log::d
                logFn(tag, "│ ◀ ${message.removePrefix("<-- ")}")
            }

            message.startsWith("<-- END") -> Log.d(tag, "└─────────────────────────────────────────────")
            message.startsWith("{") || message.startsWith("[") -> {
                try {
                    prettyGson
                        .toJson(JsonParser.parseString(message))
                        .lines()
                        .forEach { Log.d(tag, "│   $it") }
                } catch (e: Exception) {
                    Log.d(tag, "│   $message")
                    Log.d(tag, "│   $e")
                }
            }

            message.isNotBlank() -> Log.d(tag, "│   $message")
        }
    }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://apis.data.go.kr/1230000/"
    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 30L

    @Provides
    @Singleton
    @Named("serviceKey")
    fun provideServiceKey(): String = BuildConfig.GOGOV_API_KEY

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .registerTypeAdapter(BidNoticeItems::class.java, BidItemsTypeAdapter())
            .registerTypeAdapter(BidResultItems::class.java, BidResultItemsTypeAdapter())  // 추가
            .create()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        @Named("serviceKey") serviceKey: String,
    ): AuthInterceptor = AuthInterceptor(serviceKey)

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        retryInterceptor: RetryInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(buildLoggingInterceptor("HTTP_BID"))
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(retryInterceptor)
        .addNetworkInterceptor(loggingInterceptor)
        .build()

    // APK 다운로드 전용 — AuthInterceptor, RetryInterceptor 없는 클린 클라이언트
    @Provides
    @Singleton
    @Named("downloadClient")
    fun provideDownloadOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.MINUTES)  // APK 다운로드는 시간이 걸릴 수 있으므로 충분히 설정
        .followRedirects(true)              // GitHub Releases → CDN 리다이렉트 자동 추적
        .followSslRedirects(true)
        .addInterceptor { chain ->
            // GitHub CDN은 브라우저 User-Agent 없으면 504/403 반환할 수 있음
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Android)")
                .header("Accept", "application/octet-stream")
                .build()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideBidPublicInfoApi(retrofit: Retrofit): BidPublicInfoApi =
        retrofit.create(BidPublicInfoApi::class.java)

    @Provides
    @Singleton
    fun provideScsbidInfoApi(retrofit: Retrofit): ScsbidInfoApi =
        retrofit.create(ScsbidInfoApi::class.java)
}