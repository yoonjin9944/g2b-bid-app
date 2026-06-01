package com.g2b.bidapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BidResultListResponse(
    @SerializedName("response") val response: BidResultApiResponse?,
)

data class BidResultApiResponse(
    @SerializedName("header") val header: BidApiHeader?,  // BidNoticeListResponse.kt에 정의된 공통 헤더
    @SerializedName("body") val body: BidResultApiBody?,
)

data class BidResultItems(
    @SerializedName("item") val item: List<BidResultDto>?
)

data class BidResultApiBody(
    @SerializedName("items") val items: BidResultItems,
    @SerializedName("numOfRows") val numOfRows: Int,
    @SerializedName("pageNo") val pageNo: Int,
    @SerializedName("totalCount") val totalCount: Int,
)
