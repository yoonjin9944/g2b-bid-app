package com.g2b.bidapp.data.mapper

import com.g2b.bidapp.data.local.entity.WatchedBidEntity
import com.g2b.bidapp.data.supabase.dto.SupabaseBidNotice
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.BidStatus
import com.g2b.bidapp.domain.model.WatchedBid
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

private val G2B_LONG = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
private val G2B_SHORT = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

private fun String?.g2bToMillis(): Long? = this?.let { raw ->
    try {
        val fmt = if (raw.length >= 14) G2B_LONG else G2B_SHORT
        val chars = if (raw.length >= 14) 14 else 12
        LocalDateTime.parse(raw.take(chars), fmt)
            .atOffset(ZoneOffset.ofHours(9))
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

private fun String?.g2bToIso8601(): String? = this?.let { raw ->
    try {
        val fmt = if (raw.length >= 14) G2B_LONG else G2B_SHORT
        val chars = if (raw.length >= 14) 14 else 12
        LocalDateTime.parse(raw.take(chars), fmt)
            .atOffset(ZoneOffset.ofHours(9))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (_: Exception) {
        null
    }
}

private fun String?.isoToMillis(): Long? = this?.let {
    try {
        Instant.parse(it).toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

fun BidNotice.toWatchedBidEntity(
    id: String = UUID.randomUUID().toString(),
    watchedAt: Long = System.currentTimeMillis(),
): WatchedBidEntity = WatchedBidEntity(
    id = id,
    bidNtceNo = bidNtceNo,
    bidNtceNm = bidNtceNm,
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    bidNtceDt = bidNtceDt.g2bToMillis(),
    bidClseDt = bidClseDt.g2bToMillis(),
    opengDt = opengDt.g2bToMillis(),
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
    bidNtceDt = bidNtceDt.g2bToIso8601(),
    bidClseDt = bidClseDt.g2bToIso8601(),
    opengDt = opengDt.g2bToIso8601(),
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
    bidNtceNo = bidNtceNo,
    bidNtceNm = bidNtceNm,
    ntceInsttNm = ntceInsttNm,
    dmInsttNm = dmInsttNm,
    bidNtceDt = bidNtceDt.isoToMillis(),
    bidClseDt = bidClseDt.isoToMillis(),
    opengDt = opengDt.isoToMillis(),
    presmptPrce = presmptPrce,
    bdgtAmt = bdgtAmt,
    bidCategory = bidCategory,
    currentStatus = currentStatus,
    bidNtceDtlUrl = bidNtceDtlUrl,
    watchedAt = watchedAt.isoToMillis() ?: syncedAt,
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

fun WatchedBid.toEntity(): WatchedBidEntity = WatchedBidEntity(
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
    bidCategory = bidCategory.apiCode,
    currentStatus = currentStatus.name,
    bidNtceDtlUrl = bidNtceDtlUrl,
    watchedAt = watchedAt,
    syncedAt = null,
)