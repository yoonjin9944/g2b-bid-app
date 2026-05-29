package com.g2b.bidapp.ui.bid.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g2b.bidapp.domain.model.BidStatus
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.ui.components.BidStatusBadge
import com.g2b.bidapp.ui.theme.BidNoticeColor
import com.g2b.bidapp.ui.theme.StatusCancelled
import com.g2b.bidapp.util.toDisplayDateTime
import com.g2b.bidapp.util.toPriceLabel
import com.g2b.bidapp.util.toSeoulDateTime
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private enum class BidLifecycleStage {
    BIDDING,
    BIDDING_URGENT,
    AWAITING_OPEN,
    OPENED,
    UNKNOWN,
}

private fun WatchedBid.resolveLifecycleStage(): BidLifecycleStage {
    val now = LocalDateTime.now()
    val clse = bidClseDt.toSeoulDateTime() ?: return BidLifecycleStage.UNKNOWN
    val openg = opengDt.toSeoulDateTime()
    return when {
        now < clse -> {
            val days = ChronoUnit.DAYS.between(now, clse).toInt()
            if (days <= 3) BidLifecycleStage.BIDDING_URGENT else BidLifecycleStage.BIDDING
        }
        openg != null && now < openg -> BidLifecycleStage.AWAITING_OPEN
        openg != null -> BidLifecycleStage.OPENED
        else -> BidLifecycleStage.AWAITING_OPEN
    }
}

private fun WatchedBid.lifetimeFraction(): Float {
    val end = bidClseDt.toSeoulDateTime() ?: return 0f
    val now = LocalDateTime.now()
    if (now >= end) return 1f
    val start = bidNtceDt.toSeoulDateTime()
    return if (start != null && start < end) {
        val total = ChronoUnit.SECONDS.between(start, end).toFloat()
        (ChronoUnit.SECONDS.between(start, now).toFloat() / total).coerceIn(0f, 1f)
    } else {
        val daysLeft = ChronoUnit.DAYS.between(now, end).toFloat().coerceAtLeast(0f)
        (1f - (daysLeft / 60f)).coerceIn(0f, 1f)
    }
}

private fun WatchedBid.daysRemaining(): Int? {
    val clse = bidClseDt.toSeoulDateTime() ?: return null
    return ChronoUnit.DAYS.between(LocalDateTime.now(), clse).toInt()
}

@Composable
fun WatchedBidCard(
    bid: WatchedBid,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stage = remember(bid.bidNtceDt, bid.bidClseDt, bid.opengDt) {
        bid.resolveLifecycleStage()
    }
    val daysRemaining = remember(bid.bidClseDt) { bid.daysRemaining() }
    val lifetimeFraction = remember(bid.bidNtceDt, bid.bidClseDt) { bid.lifetimeFraction() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 라이프사이클 진행상태 배지 (투찰중/마감임박/개찰 대기/개찰)
                    LifecycleStageBadge(stage = stage)

                    // 상태 변경 배지 — REGISTERED가 아닐 때만 표시 (변경/취소/재공고/개찰 알림용)
                    if (bid.currentStatus != BidStatus.REGISTERED) {
                        BidStatusBadge(status = bid.currentStatus)
                    }
                }
                Text(
                    text = "공고번호: ${bid.bidNtceNo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF737780),
                    maxLines = 1,
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = bid.bidNtceNm,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                ),
                color = Color(0xFF0B1C30),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Business, null,
                    tint = BidNoticeColor,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "수요기관: ${bid.dmInsttNm ?: bid.ntceInsttNm ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BidNoticeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule, null,
                    tint = BidNoticeColor,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "마감일시: ${bid.bidClseDt.toDisplayDateTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF43474F),
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFDCE9FF))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DeadlineProgressBar(stage = stage, fraction = lifetimeFraction)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (stage) {
                            BidLifecycleStage.UNKNOWN -> ""
                            BidLifecycleStage.AWAITING_OPEN -> "개찰 대기"
                            BidLifecycleStage.OPENED -> "개찰"
                            else -> if (daysRemaining != null && daysRemaining >= 0) "D-$daysRemaining" else "마감"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                        ),
                        color = stage.textColor(),
                    )
                }

                Text(
                    text = bid.presmptPrce?.toPriceLabel() ?: "-",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF0B1C30),
                )
            }
        }
    }
}

@Composable
private fun LifecycleStageBadge(stage: BidLifecycleStage) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(stage.containerColor())
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = stage.label(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.6.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = stage.contentColor(),
        )
    }
}

@Composable
private fun DeadlineProgressBar(stage: BidLifecycleStage, fraction: Float) {
    val isPostBid = stage == BidLifecycleStage.AWAITING_OPEN || stage == BidLifecycleStage.OPENED
    val barColor = if (isPostBid) Color(0xFF94A3B8) else stage.barColor()
    val barFraction = if (isPostBid) 1f else fraction
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(8.dp)
            .clip(CircleShape)
            .background(Color(0xFFDCE9FF)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(barFraction)
                .height(8.dp)
                .clip(CircleShape)
                .background(barColor),
        )
    }
}

private fun BidLifecycleStage.label() = when (this) {
    BidLifecycleStage.BIDDING -> "투찰중"
    BidLifecycleStage.BIDDING_URGENT -> "마감임박"
    BidLifecycleStage.AWAITING_OPEN -> "개찰 대기"
    BidLifecycleStage.OPENED -> "개찰"
    BidLifecycleStage.UNKNOWN -> "공고중"
}

private fun BidLifecycleStage.containerColor() = when (this) {
    BidLifecycleStage.BIDDING, BidLifecycleStage.UNKNOWN -> Color(0xFF001E40)
    BidLifecycleStage.BIDDING_URGENT -> Color(0xFFFFDAD6)
    BidLifecycleStage.AWAITING_OPEN, BidLifecycleStage.OPENED -> Color(0xFFECEFF1)
}

private fun BidLifecycleStage.contentColor() = when (this) {
    BidLifecycleStage.BIDDING, BidLifecycleStage.UNKNOWN -> Color(0xFF799DD6)
    BidLifecycleStage.BIDDING_URGENT -> StatusCancelled
    BidLifecycleStage.AWAITING_OPEN, BidLifecycleStage.OPENED -> Color(0xFF6E7680)
}

private fun BidLifecycleStage.textColor() = when (this) {
    BidLifecycleStage.BIDDING_URGENT -> StatusCancelled
    BidLifecycleStage.AWAITING_OPEN, BidLifecycleStage.OPENED -> Color(0xFF6E7680)
    else -> Color(0xFF0060AC)
}

private fun BidLifecycleStage.barColor() = when (this) {
    BidLifecycleStage.BIDDING_URGENT -> StatusCancelled
    else -> Color(0xFF0060AC)
}

private fun sampleWatchedBid() = WatchedBid(
    id = "sample-id",
    bidNtceNo = "20240001234",
    bidNtceNm = "2024년도 시설물 유지보수 공사 (샘플 공고명)",
    ntceInsttNm = "서울특별시",
    dmInsttNm = "서울특별시 강남구",
    bidNtceDt = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L,
    bidClseDt = System.currentTimeMillis() + 5 * 24 * 3600 * 1000L,
    opengDt = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L,
    presmptPrce = 250000000L,
    bdgtAmt = null,
    bidCategory = com.g2b.bidapp.domain.model.BidCategory.CNSTWK,
    currentStatus = BidStatus.REGISTERED,
    bidNtceDtlUrl = null,
    watchedAt = System.currentTimeMillis(),
    syncedAt = System.currentTimeMillis(),
)

@Preview(showBackground = true, name = "WatchedBidCard - 투찰중")
@Composable
private fun WatchedBidCardBiddingPreview() {
    WatchedBidCard(bid = sampleWatchedBid(), onClick = {})
}

@Preview(showBackground = true, name = "WatchedBidCard - 변경됨")
@Composable
private fun WatchedBidCardChangedPreview() {
    WatchedBidCard(bid = sampleWatchedBid().copy(currentStatus = BidStatus.CHANGED), onClick = {})
}

@Preview(showBackground = true, name = "WatchedBidCard - 마감임박")
@Composable
private fun WatchedBidCardUrgentPreview() {
    WatchedBidCard(
        bid = sampleWatchedBid().copy(
            bidClseDt = System.currentTimeMillis() + 2 * 24 * 3600 * 1000L,
        ),
        onClick = {},
    )
}
