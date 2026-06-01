package com.g2b.bidapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BidNoticeDto(
    @SerializedName("bidNtceNo") val bidNtceNo: String?,
    @SerializedName("bidNtceOrd") val bidNtceOrd: String?,
    @SerializedName("bidNtceNm") val bidNtceNm: String?,
    @SerializedName("ntceInsttNm") val ntceInsttNm: String?,
    @SerializedName("dmInsttNm") val dmInsttNm: String?,
    @SerializedName("bidNtceDt") val bidNtceDt: String?,
    @SerializedName("bidClseDt") val bidClseDt: String?,
    @SerializedName("opengDt") val opengDt: String?,
    @SerializedName("presmptPrce") val presmptPrce: String?,
    @SerializedName("bdgtAmt") val bdgtAmt: String?,
    @SerializedName("ntceKindNm") val ntceKindNm: String?,
    @SerializedName("bidNtceDtlUrl") val bidNtceDtlUrl: String?,
)
