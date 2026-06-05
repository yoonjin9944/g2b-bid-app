package com.g2b.bidapp.domain.model

data class BidNotice(
    val bidNtceNo: String,
    val bidNtceOrd: String,
    val bidNtceNm: String,
    val ntceInsttNm: String?,
    val dmInsttNm: String?,
    val bidNtceDt: String?,
    val bidClseDt: String?,
    val opengDt: String?,
    val presmptPrce: Long?,
    val bdgtAmt: Long?,
    val ntceKindNm: String?,
    val bidCategory: BidCategory,
    val bidNtceDtlUrl: String?,
    val isWatched: Boolean = false,
)
