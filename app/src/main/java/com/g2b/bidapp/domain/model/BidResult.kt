package com.g2b.bidapp.domain.model

// 낙찰결과 도메인 모델
data class BidResult(
    val bidNtceNo: String,
    val bidNtceOrd: String,
    val bidNtceNm: String,
    val ntceInsttNm: String?,
    val dmInsttNm: String?,
    val opengDt: String?,       // 개찰일시 (yyyyMMddHHmm)
    val scsbidNm: String?,      // 낙찰업체명
    val scsbidAmt: Long?,       // 낙찰금액 (원)
    val presmptPrce: Long?,     // 추정가격 (원)
    val bdgtAmt: Long?,         // 예산금액 (원)
    val sucsfbidRate: String?,  // 낙찰율 (%)
    val bidCategory: BidCategory,
    val bidNtceDtlUrl: String?,
)