package com.g2b.bidapp.di

import com.g2b.bidapp.data.repository.AuthRepositoryImpl
import com.g2b.bidapp.data.repository.BidRepositoryImpl
import com.g2b.bidapp.data.repository.WatchlistRepositoryImpl
import com.g2b.bidapp.domain.repository.AuthRepository
import com.g2b.bidapp.domain.repository.BidRepository
import com.g2b.bidapp.domain.repository.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBidRepository(impl: BidRepositoryImpl): BidRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository
}