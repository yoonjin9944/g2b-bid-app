package com.g2b.bidapp.data.mapper

import com.g2b.bidapp.data.remote.dto.BidNoticeDto
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice

fun BidNoticeDto.toModel(category: BidCategory): BidNotice = BidNotice(
    bidNtceNo = bidNtceNo.orEmpty(),
    bidNtceOrd = bidNtceOrd.orEmpty(),
    bidNtceNm = bidNtceNm.orEmpty(),
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    bidNtceDt = bidNtceDt,
    bidClseDt = bidClseDt,
    opengDt = opengDt,
    presmptPrce = presmptPrce?.toLongOrNull(),
    bdgtAmt = bdgtAmt?.toLongOrNull(),
    bidCategory = category,
    bidNtceDtlUrl = bidNtceDtlUrl,
)

fun Long.toPriceLabel(): String {
    if (this <= 0L) return "-"
    val uk = this / 100_000_000L
    val man = (this % 100_000_000L) / 10_000L
    return buildString {
        if (uk > 0) append("${uk}억 ")
        if (man > 0) append("${man}만")
        if (uk == 0L && man == 0L) append("${this@toPriceLabel}원")
        else append("원")
    }.trim()
}