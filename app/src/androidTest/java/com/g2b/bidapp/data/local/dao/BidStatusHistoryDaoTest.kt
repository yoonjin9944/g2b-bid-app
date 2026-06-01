package com.g2b.bidapp.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.g2b.bidapp.data.local.G2bDatabase
import com.g2b.bidapp.data.local.entity.BidStatusHistoryEntity
import com.g2b.bidapp.data.local.entity.WatchedBidEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BidStatusHistoryDaoTest {

    private lateinit var db: G2bDatabase
    private lateinit var watchedBidDao: WatchedBidDao
    private lateinit var historyDao: BidStatusHistoryDao

    private val parentEntity = WatchedBidEntity(
        id = "parent-uuid",
        bidNtceNo = "20240101001",
        bidNtceNm = "테스트 공고",
        ntceInsttNm = null,
        dmInsttNm = null,
        bidNtceDt = null,
        bidClseDt = null,
        opengDt = null,
        presmptPrce = null,
        bdgtAmt = null,
        bidCategory = "CNSTWK",
        currentStatus = "REGISTERED",
        bidNtceDtlUrl = null,
        watchedAt = System.currentTimeMillis(),
        syncedAt = null,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            G2bDatabase::class.java,
        ).allowMainThreadQueries().build()
        watchedBidDao = db.watchedBidDao()
        historyDao = db.bidStatusHistoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun 이력_INSERT_후_해당_공고의_Flow에_포함된다() = runTest {
        watchedBidDao.insertOrIgnore(parentEntity)

        val history = BidStatusHistoryEntity(
            id = "history-uuid-1",
            watchedBidId = parentEntity.id,
            previousStatus = "REGISTERED",
            newStatus = "CHANGED",
            detectedAt = System.currentTimeMillis(),
        )
        historyDao.insert(history)

        historyDao.getByWatchedBidIdFlow(parentEntity.id).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("CHANGED", list[0].newStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun 부모_공고_삭제_시_이력도_CASCADE_삭제된다() = runTest {
        watchedBidDao.insertOrIgnore(parentEntity)

        historyDao.insert(
            BidStatusHistoryEntity(
                id = "history-uuid-1",
                watchedBidId = parentEntity.id,
                previousStatus = "REGISTERED",
                newStatus = "CANCELLED",
                detectedAt = System.currentTimeMillis(),
            )
        )

        watchedBidDao.deleteByBidNtceNo(parentEntity.bidNtceNo)

        historyDao.getByWatchedBidIdFlow(parentEntity.id).test {
            val list = awaitItem()
            assertEquals(0, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

}