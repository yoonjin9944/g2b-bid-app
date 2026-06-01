package com.g2b.bidapp.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.g2b.bidapp.data.local.G2bDatabase
import com.g2b.bidapp.data.local.entity.WatchedBidEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchedBidDaoTest {

    private lateinit var db: G2bDatabase
    private lateinit var dao: WatchedBidDao

    private fun makeEntity(
        id: String = "uuid-1",
        bidNtceNo: String = "20240101001",
        bidNtceNm: String = "테스트 공고",
    ) = WatchedBidEntity(
        id = id,
        bidNtceNo = bidNtceNo,
        bidNtceNm = bidNtceNm,
        ntceInsttNm = "테스트 기관",
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
        dao = db.watchedBidDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun INSERT_후_Flow_방출에_항목이_포함된다() = runTest {
        val entity = makeEntity()
        dao.insertOrIgnore(entity)

        dao.getAllFlow().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(entity.bidNtceNo, list[0].bidNtceNo)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun 동일_bid_ntce_no_중복_INSERT는_무시된다() = runTest {
        val entity = makeEntity()
        dao.insertOrIgnore(entity)
        val result = dao.insertOrIgnore(entity.copy(id = "uuid-2"))  // 다른 id지만 동일 bidNtceNo

        assertEquals(-1L, result)  // IGNORE 시 -1 반환

        dao.getAllFlow().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun bid_ntce_no로_삭제하면_Flow에서_제거된다() = runTest {
        val entity = makeEntity()
        dao.insertOrIgnore(entity)
        dao.deleteByBidNtceNo(entity.bidNtceNo)

        dao.getAllFlow().test {
            val list = awaitItem()
            assertEquals(0, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByBidNtceNo로_존재하는_항목을_조회한다() = runTest {
        val entity = makeEntity()
        dao.insertOrIgnore(entity)

        val found = dao.getByBidNtceNo(entity.bidNtceNo)
        assertNotNull(found)
        assertEquals(entity.bidNtceNo, found!!.bidNtceNo)
    }

    @Test
    fun 존재하지_않는_bid_ntce_no_조회_시_null을_반환한다() = runTest {
        val found = dao.getByBidNtceNo("NOT_EXIST")
        assertNull(found)
    }

    @Test
    fun 키워드_필터_쿼리가_정확하게_동작한다() = runTest {
        dao.insertOrIgnore(makeEntity(id = "1", bidNtceNo = "001", bidNtceNm = "서울 도로 공사"))
        dao.insertOrIgnore(makeEntity(id = "2", bidNtceNo = "002", bidNtceNm = "부산 건물 유지보수"))
        dao.insertOrIgnore(makeEntity(id = "3", bidNtceNo = "003", bidNtceNm = "서울 교량 점검"))

        dao.getByKeywordFlow("서울").test {
            val list = awaitItem()
            assertEquals(2, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun syncedAt_업데이트_후_해당_행만_변경된다() = runTest {
        dao.insertOrIgnore(makeEntity(id = "uuid-1", bidNtceNo = "001"))
        dao.insertOrIgnore(makeEntity(id = "uuid-2", bidNtceNo = "002"))

        val now = System.currentTimeMillis()
        dao.updateSyncedAt("uuid-1", now)

        val updated = dao.getByBidNtceNo("001")
        val unchanged = dao.getByBidNtceNo("002")

        assertNotNull(updated?.syncedAt)
        assertNull(unchanged?.syncedAt)
    }

    @Test
    fun getAllBidNtceNos가_모든_공고번호를_반환한다() = runTest {
        dao.insertOrIgnore(makeEntity(id = "1", bidNtceNo = "A001"))
        dao.insertOrIgnore(makeEntity(id = "2", bidNtceNo = "A002"))

        val nos = dao.getAllBidNtceNos()
        assertEquals(2, nos.size)
        assert("A001" in nos)
        assert("A002" in nos)
    }
}