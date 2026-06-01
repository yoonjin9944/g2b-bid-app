package com.g2b.bidapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BidNoticeListResponse(
    @SerializedName("response") val response: BidApiResponse?,
)

data class BidApiResponse(
    @SerializedName("header") val header: BidApiHeader?,
    @SerializedName("body") val body: BidApiBody?,
)

data class BidApiHeader(
    @SerializedName("resultCode") val resultCode: String?,
    @SerializedName("resultMsg") val resultMsg: String?,
)

data class BidNoticeItems(
    @SerializedName("item") val item: List<BidNoticeDto>?
)

data class BidApiBody(
    @SerializedName("items") val items: BidNoticeItems,
    @SerializedName("numOfRows") val numOfRows: Int,
    @SerializedName("pageNo") val pageNo: Int,
    @SerializedName("totalCount") val totalCount: Int,
)
