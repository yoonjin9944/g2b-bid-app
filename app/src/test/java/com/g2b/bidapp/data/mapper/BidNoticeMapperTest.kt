package com.g2b.bidapp.data.mapper

import com.g2b.bidapp.data.remote.dto.BidNoticeDto
import com.g2b.bidapp.domain.model.BidCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BidNoticeMapperTest {

    private fun dto(
        bidNtceNo: String? = "20231015678",
        bidNtceOrd: String? = "00",
        bidNtceNm: String? = "테스트 공고",
        presmptPrce: String? = null,
    ) = BidNoticeDto(
        bidNtceNo = bidNtceNo,
        bidNtceOrd = bidNtceOrd,
        bidNtceNm = bidNtceNm,
        ntceInsttNm = "조달청",
        dmInsttNm = "행정안전부",
        bidNtceDt = "20231015140000",
        bidClseDt = "20231115140000",
        opengDt = null,
        presmptPrce = presmptPrce,
        bdgtAmt = null,
        ntceKindNm = null,
        bidNtceDtlUrl = null,
    )

    @Test
    fun `presmptPrce 가 null 이면 Domain 모델에서 null 반환`() {
        val notice = dto(presmptPrce = null).toModel(BidCategory.CNSTWK)
        assertNull(notice.presmptPrce)
    }

    @Test
    fun `presmptPrce 가 숫자 문자열이면 Long 으로 변환`() {
        val notice = dto(presmptPrce = "150000000").toModel(BidCategory.CNSTWK)
        assertEquals(150_000_000L, notice.presmptPrce)
    }

    @Test
    fun `presmptPrce 가 파싱 불가 문자열이면 null 반환`() {
        val notice = dto(presmptPrce = "N/A").toModel(BidCategory.CNSTWK)
        assertNull(notice.presmptPrce)
    }

    @Test
    fun `bidCategory 가 올바르게 설정된다`() {
        val notice = dto().toModel(BidCategory.SERVC)
        assertEquals(BidCategory.SERVC, notice.bidCategory)
    }

    @Test
    fun `bidNtceNo 가 null 이면 빈 문자열로 변환`() {
        val notice = dto(bidNtceNo = null).toModel(BidCategory.CNSTWK)
        assertEquals("", notice.bidNtceNo)
    }

    @Test
    fun `toPriceLabel 1억원 변환`() {
        assertEquals("1억 원", 100_000_000L.toPriceLabel())
    }

    @Test
    fun `toPriceLabel 1억5000만원 변환`() {
        assertEquals("1억 5000만원", 150_000_000L.toPriceLabel())
    }

    @Test
    fun `toPriceLabel 0원은 대시 반환`() {
        assertEquals("-", 0L.toPriceLabel())
    }
}