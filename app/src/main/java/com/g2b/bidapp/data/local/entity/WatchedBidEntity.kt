package com.g2b.bidapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watched_bids",
    indices = [Index(value = ["bid_ntce_no"], unique = true)],
)

data class WatchedBidEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "bid_ntce_no") val bidNtceNo: String,
    @ColumnInfo(name = "bid_ntce_nm") val bidNtceNm: String,
    @ColumnInfo(name = "ntce_instt_nm") val ntceInsttNm: String?,
    @ColumnInfo(name = "dminstt_nm") val dmInsttNm: String?,
    @ColumnInfo(name = "bid_ntce_dt") val bidNtceDt: Long?,
    @ColumnInfo(name = "bid_clse_dt") val bidClseDt: Long?,
    @ColumnInfo(name = "openg_dt") val opengDt: Long?,
    @ColumnInfo(name = "presmpt_prce") val presmptPrce: Long?,
    @ColumnInfo(name = "bdgt_amt") val bdgtAmt: Long?,
    @ColumnInfo(name = "bid_category") val bidCategory: String,
    @ColumnInfo(name = "current_status") val currentStatus: String,
    @ColumnInfo(name = "bid_ntce_dtl_url") val bidNtceDtlUrl: String?,
    @ColumnInfo(name = "watched_at") val watchedAt: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?,
)
