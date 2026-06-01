package com.g2b.bidapp.domain.model

data class WatchedBid(
    val id: String,             // 로컬 UUID
    val bidNtceNo: String,
    val bidNtceNm: String,
    val ntceInsttNm: String?,
    val dmInsttNm: String?,
    val bidNtceDt: Long?,
    val bidClseDt: Long?,
    val opengDt: Long?,
    val presmptPrce: Long?,
    val bdgtAmt: Long?,
    val bidCategory: BidCategory,
    val currentStatus: BidStatus,
    val bidNtceDtlUrl: String?,
    val watchedAt: Long,
    val syncedAt: Long?,
)
