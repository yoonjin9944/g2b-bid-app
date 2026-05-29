package com.g2b.bidapp.data.mapper

import com.g2b.bidapp.data.remote.dto.BidResultDto
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidResult

fun BidResultDto.toModel(category: BidCategory): BidResult = BidResult(
    bidNtceNo = bidNtceNo.orEmpty(),
    bidNtceOrd = bidNtceOrd ?: "000",
    bidNtceNm = bidNtceNm.orEmpty(),
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    opengDt = opengDt,
    scsbidNm = scsbidNm,
    scsbidAmt = scsbidAmt?.toLongOrNull(),
    presmptPrce = presmptPrce?.toLongOrNull(),
    bdgtAmt = bdgtAmt?.toLongOrNull(),
    sucsfbidRate = sucsfbidRate,
    bidCategory = category,
    bidNtceDtlUrl = bidNtceDtlUrl,
)