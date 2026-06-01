package com.g2b.bidapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// 낙찰결과 단건 DTO
// 실제 API 응답 필드명은 조달청 API 명세서(낙찰정보서비스.htm) 확인 후 조정 필요
data class BidResultDto(
    @SerializedName("bidNtceNo") val bidNtceNo: String?,
    @SerializedName("bidNtceOrd") val bidNtceOrd: String?,
    @SerializedName("bidNtceNm") val bidNtceNm: String?,
    @SerializedName("ntceInsttNm") val ntceInsttNm: String?,
    @SerializedName("dmInsttNm") val dmInsttNm: String?,
    @SerializedName("opengDt") val opengDt: String?,          // 개찰일시 (yyyyMMddHHmm)
    @SerializedName("scsbidNm") val scsbidNm: String?,         // 낙찰업체명
    @SerializedName("scsbidAmt") val scsbidAmt: String?,       // 낙찰금액 (원)
    @SerializedName("presmptPrce") val presmptPrce: String?,   // 추정가격 (원)
    @SerializedName("bdgtAmt") val bdgtAmt: String?,           // 예산금액 (원)
    @SerializedName("sucsfbidRate") val sucsfbidRate: String?, // 낙찰율 (%)
    @SerializedName("bidNtceDtlUrl") val bidNtceDtlUrl: String?,
)