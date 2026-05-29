package com.g2b.bidapp.data.mapper

import com.g2b.bidapp.data.local.entity.WatchedBidEntity
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.BidStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WatchedBidMapperTest {

    private val sampleNotice = BidNotice(
        bidNtceNo = "20240101001",
        bidNtceOrd = "00",
        bidNtceNm = "테스트 공고",
        ntceInsttNm = "테스트 기관",
        dmInsttNm = null,
        bidNtceDt = "20240101120000",
        bidClseDt = "20240201180000",
        opengDt = null,
        presmptPrce = 100_000_000L,
        bdgtAmt = null,
        bidCategory = BidCategory.CNSTWK,
        bidNtceDtlUrl = "https://www.g2b.go.kr",
    )

    @Test
    fun `BidNotice를 WatchedBidEntity로 변환한다`() {
        val entity = sampleNotice.toWatchedBidEntity(userId = "test-user-id")

        assertEquals(sampleNotice.bidNtceNo, entity.bidNtceNo)
        assertEquals(sampleNotice.bidNtceNm, entity.bidNtceNm)
        assertEquals(sampleNotice.bidCategory.apiCode, entity.bidCategory)
        assertEquals(BidStatus.REGISTERED.name, entity.currentStatus)
        assertNotNull(entity.bidNtceDt)  // "20240101120000" → Unix millis
        assertNotNull(entity.bidClseDt)
        assertNull(entity.syncedAt)      // 등록 직후는 미동기화 상태
    }

    @Test
    fun `날짜가 null인 BidNotice 변환 시 해당 필드가 null이다`() {
        val noDateNotice = sampleNotice.copy(bidNtceDt = null, bidClseDt = null)
        val entity = noDateNotice.toWatchedBidEntity()

        assertNull(entity.bidNtceDt)
        assertNull(entity.bidClseDt)
    }

    @Test
    fun `WatchedBidEntity를 WatchedBid domain 모델로 변환한다`() {
        val entity = WatchedBidEntity(
            id = "uuid-1",
            userId = "test-user-id",
            bidNtceNo = "20240101001",
            bidNtceNm = "테스트 공고",
            ntceInsttNm = "테스트 기관",
            dmInsttNm = null,
            bidNtceDt = 1704067200000L,
            bidClseDt = null,
            opengDt = null,
            presmptPrce = 100_000_000L,
            bdgtAmt = null,
            bidCategory = "CNSTWK",
            currentStatus = "REGISTERED",
            bidNtceDtlUrl = null,
            watchedAt = System.currentTimeMillis(),
            syncedAt = null,
        )

        val model = entity.toModel()

        assertEquals(entity.id, model.id)
        assertEquals(entity.bidNtceNo, model.bidNtceNo)
        assertEquals(BidCategory.CNSTWK, model.bidCategory)
        assertEquals(BidStatus.REGISTERED, model.currentStatus)
        assertNull(model.syncedAt)
    }

    @Test
    fun `알 수 없는 currentStatus는 REGISTERED로 폴백된다`() {
        val entity = WatchedBidEntity(
            id = "uuid-1",
            userId = "test-user-id",
            bidNtceNo = "001",
            bidNtceNm = "공고",
            ntceInsttNm = null,
            dmInsttNm = null,
            bidNtceDt = null,
            bidClseDt = null,
            opengDt = null,
            presmptPrce = null,
            bdgtAmt = null,
            bidCategory = "CNSTWK",
            currentStatus = "UNKNOWN_STATUS",
            bidNtceDtlUrl = null,
            watchedAt = System.currentTimeMillis(),
            syncedAt = null,
        )

        val model = entity.toModel()
        assertEquals(BidStatus.REGISTERED, model.currentStatus)
    }
}