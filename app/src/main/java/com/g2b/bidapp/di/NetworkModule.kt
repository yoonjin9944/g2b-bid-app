package com.g2b.bidapp.di

import com.g2b.bidapp.BuildConfig
import com.g2b.bidapp.data.remote.api.BidPublicInfoApi
import com.g2b.bidapp.data.remote.dto.BidItemsTypeAdapter
import com.g2b.bidapp.data.remote.dto.BidNoticeDto
import com.g2b.bidapp.data.remote.dto.BidNoticeItems
import com.g2b.bidapp.data.remote.interceptor.AuthInterceptor
import com.g2b.bidapp.data.remote.interceptor.RetryInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
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
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(retryInterceptor)
        .addNetworkInterceptor(loggingInterceptor)
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
}