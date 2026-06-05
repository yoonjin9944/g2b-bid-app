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
    ntceKindNm = ntceKindNm,
    bidCategory = category,
    bidNtceDtlUrl = bidNtceDtlUrl,
)

