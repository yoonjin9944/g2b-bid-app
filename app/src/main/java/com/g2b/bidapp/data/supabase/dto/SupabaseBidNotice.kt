package com.g2b.bidapp.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseBidNotice(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("bid_ntce_no") val bidNtceNo: String,
    @SerialName("bid_ntce_nm") val bidNtceNm: String,
    @SerialName("ntce_instt_nm") val ntceInsttNm: String? = null,
    @SerialName("dminstt_nm") val dmInsttNm: String? = null,
    @SerialName("bid_ntce_dt") val bidNtceDt: String? = null,
    @SerialName("bid_clse_dt") val bidClseDt: String? = null,
    @SerialName("openg_dt") val opengDt: String? = null,
    @SerialName("presmpt_prce") val presmptPrce: Long? = null,
    @SerialName("bdgt_amt") val bdgtAmt: Long? = null,
    @SerialName("bid_category") val bidCategory: String,
    @SerialName("current_status") val currentStatus: String = "REGISTERED",
    @SerialName("bid_ntce_dtl_url") val bidNtceDtlUrl: String? = null,
    @SerialName("watched_at") val watchedAt: String? = null,
)
