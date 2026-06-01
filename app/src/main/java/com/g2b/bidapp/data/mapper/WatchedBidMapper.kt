package com.g2b.bidapp.data.mapper

import com.g2b.bidapp.data.local.entity.WatchedBidEntity
import com.g2b.bidapp.data.supabase.dto.SupabaseBidNotice
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.BidStatus
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.util.parseIsoToMillis
import com.g2b.bidapp.util.toG2bIso8601
import com.g2b.bidapp.util.toG2bMillis
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

fun BidNotice.toWatchedBidEntity(
    userId: String,
    id: String = UUID.randomUUID().toString(),
    watchedAt: Long = System.currentTimeMillis(),
): WatchedBidEntity = WatchedBidEntity(
    id = id,
    userId = userId,
    bidNtceNo = bidNtceNo,
    bidNtceNm = bidNtceNm,
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    bidNtceDt = bidNtceDt.toG2bMillis(),
    bidClseDt = bidClseDt.toG2bMillis(),
    opengDt = opengDt.toG2bMillis(),
    presmptPrce = presmptPrce,
    bdgtAmt = bdgtAmt,
    bidCategory = bidCategory.apiCode,
    currentStatus = BidStatus.REGISTERED.name,
    bidNtceDtlUrl = bidNtceDtlUrl,
    watchedAt = watchedAt,
    syncedAt = null,
)

fun BidNotice.toSupabaseBidNotice(userId: String): SupabaseBidNotice = SupabaseBidNotice(
    userId = userId,
    bidNtceNo = bidNtceNo,
    bidNtceNm = bidNtceNm,
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    bidNtceDt = bidNtceDt.toG2bIso8601(),
    bidClseDt = bidClseDt.toG2bIso8601(),
    opengDt = opengDt.toG2bIso8601(),
    presmptPrce = presmptPrce,
    bdgtAmt = bdgtAmt,
    bidCategory = bidCategory.apiCode,
    bidNtceDtlUrl = bidNtceDtlUrl,
)

fun WatchedBidEntity.toModel(): WatchedBid = WatchedBid(
    id = id,
    bidNtceNo = bidNtceNo,
    bidNtceNm = bidNtceNm,
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    bidNtceDt = bidNtceDt,
    bidClseDt = bidClseDt,
    opengDt = opengDt,
    presmptPrce = presmptPrce,
    bdgtAmt = bdgtAmt,
    bidCategory = BidCategory.fromApiCode(bidCategory),
    currentStatus = BidStatus.entries.firstOrNull { it.name == currentStatus }
        ?: BidStatus.REGISTERED,
    bidNtceDtlUrl = bidNtceDtlUrl,
    watchedAt = watchedAt,
    syncedAt = syncedAt,
)

fun SupabaseBidNotice.toWatchedBidEntity(
    syncedAt: Long = System.currentTimeMillis(),
): WatchedBidEntity = WatchedBidEntity(
    id = id ?: UUID.randomUUID().toString(),
    userId = userId ?: "",
    bidNtceNo = bidNtceNo,
    bidNtceNm = bidNtceNm,
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    bidNtceDt = bidNtceDt.parseIsoToMillis(),
    bidClseDt = bidClseDt.parseIsoToMillis(),
    opengDt = opengDt.parseIsoToMillis(),
    presmptPrce = presmptPrce,
    bdgtAmt = bdgtAmt,
    bidCategory = bidCategory,
    currentStatus = currentStatus,
    bidNtceDtlUrl = bidNtceDtlUrl,
    watchedAt = watchedAt.parseIsoToMillis() ?: syncedAt,
    syncedAt = syncedAt,
)

fun WatchedBidEntity.toSupabaseBidNotice(userId: String): SupabaseBidNotice {
    fun Long?.toIso() = this?.let {
        Instant.ofEpochMilli(it)
            .atOffset(ZoneOffset.ofHours(9))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
    return SupabaseBidNotice(
        id = id,
        userId = userId,
        bidNtceNo = bidNtceNo,
        bidNtceNm = bidNtceNm,
        ntceInsttNm = ntceInsttNm,
        dmInsttNm = dmInsttNm,
        bidNtceDt = bidNtceDt.toIso(),
        bidClseDt = bidClseDt.toIso(),
        opengDt = opengDt.toIso(),
        presmptPrce = presmptPrce,
        bdgtAmt = bdgtAmt,
        bidCategory = bidCategory,
        currentStatus = currentStatus,
        bidNtceDtlUrl = bidNtceDtlUrl,
        watchedAt = watchedAt.toIso(),
    )
}

fun WatchedBid.toEntity(userId: String): WatchedBidEntity = WatchedBidEntity(
    id = id,
    userId = userId,
    bidNtceNo = bidNtceNo,
    bidNtceNm = bidNtceNm,
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    bidNtceDt = bidNtceDt,
    bidClseDt = bidClseDt,
    opengDt = opengDt,
    presmptPrce = presmptPrce,
    bdgtAmt = bdgtAmt,
    bidCategory = bidCategory.apiCode,
    currentStatus = currentStatus.name,
    bidNtceDtlUrl = bidNtceDtlUrl,
    watchedAt = watchedAt,
    syncedAt = null,
)
