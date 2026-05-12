package com.g2b.bidapp.di

import com.g2b.bidapp.data.repository.AuthRepositoryImpl
import com.g2b.bidapp.domain.repository.AuthRepository
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
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): AuthRepository
}