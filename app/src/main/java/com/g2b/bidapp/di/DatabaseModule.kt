package com.g2b.bidapp.di

import android.content.Context
import androidx.room.Room
import com.g2b.bidapp.data.local.G2bDatabase
import com.g2b.bidapp.data.local.dao.BidStatusHistoryDao
import com.g2b.bidapp.data.local.dao.NotificationDao
import com.g2b.bidapp.data.local.dao.WatchedBidDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "g2b_bid_database"

    @Provides
    @Singleton
    fun provideG2bDatabase(
        @ApplicationContext context: Context,
    ): G2bDatabase =
        Room.databaseBuilder(
            context,
            G2bDatabase::class.java,
            DB_NAME,
        )
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()

    @Provides
    @Singleton
    fun provideWatchedBidDao(db: G2bDatabase): WatchedBidDao = db.watchedBidDao()

    @Provides
    @Singleton
    fun provideBidStatusHistoryDao(db: G2bDatabase): BidStatusHistoryDao = db.bidStatusHistoryDao()

    @Provides
    @Singleton
    fun provideNotificationDao(db: G2bDatabase): NotificationDao = db.notificationDao()
}