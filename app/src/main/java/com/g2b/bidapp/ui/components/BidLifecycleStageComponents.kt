package com.g2b.bidapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g2b.bidapp.ui.theme.StatusCancelled

// ─── 공고 라이프사이클 단계 ────────────────────────────────────────────────────

enum class BidLifecycleStage {
    BIDDING,        // 투찰 진행 중 (now < bidClseDt)
    BIDDING_URGENT, // 마감임박 (3일 이하)
    AWAITING_OPEN,  // 개찰 대기 (bidClseDt <= now < opengDt)
    OPENED,         // 개찰일 경과
    UNKNOWN,        // 날짜 정보 없음
}

fun BidLifecycleStage.label() = when (this) {
    BidLifecycleStage.BIDDING        -> "투찰중"
    BidLifecycleStage.BIDDING_URGENT -> "마감임박"
    BidLifecycleStage.AWAITING_OPEN  -> "개찰 대기"
    BidLifecycleStage.OPENED         -> "개찰"
    BidLifecycleStage.UNKNOWN        -> "공고중"
}

fun BidLifecycleStage.containerColor() = when (this) {
    BidLifecycleStage.BIDDING, BidLifecycleStage.UNKNOWN        -> Color(0xFF001E40)
    BidLifecycleStage.BIDDING_URGENT                            -> Color(0xFFFFDAD6)
    BidLifecycleStage.AWAITING_OPEN, BidLifecycleStage.OPENED  -> Color(0xFFECEFF1)
}

fun BidLifecycleStage.contentColor() = when (this) {
    BidLifecycleStage.BIDDING, BidLifecycleStage.UNKNOWN        -> Color(0xFF799DD6)
    BidLifecycleStage.BIDDING_URGENT                            -> StatusCancelled
    BidLifecycleStage.AWAITING_OPEN, BidLifecycleStage.OPENED  -> Color(0xFF6E7680)
}

fun BidLifecycleStage.textColor() = when (this) {
    BidLifecycleStage.BIDDING_URGENT                           -> StatusCancelled
    BidLifecycleStage.AWAITING_OPEN, BidLifecycleStage.OPENED -> Color(0xFF6E7680)
    else                                                       -> Color(0xFF0060AC)
}

fun BidLifecycleStage.barColor() = when (this) {
    BidLifecycleStage.BIDDING_URGENT -> StatusCancelled
    else                             -> Color(0xFF0060AC)
}

// 프로그레스바 만점 기준: 15일 이상 남으면 0%, 마감이면 100%
const val DEADLINE_FULL_DAYS = 15f

// ─── 공유 Composable ──────────────────────────────────────────────────────────

/**
 * 라이프사이클 단계 배지 (투찰중 / 마감임박 / 개찰 대기 / 개찰 / 공고중)
 */
@Composable
fun LifecycleStageBadge(stage: BidLifecycleStage, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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

/**
 * 마감일 기준 진행률 프로그레스 바
 */
@Composable
fun DeadlineProgressBar(stage: BidLifecycleStage, fraction: Float, modifier: Modifier = Modifier) {
    val isPostBid = stage == BidLifecycleStage.AWAITING_OPEN || stage == BidLifecycleStage.OPENED
    val barColor   = if (isPostBid) Color(0xFF94A3B8) else stage.barColor()
    val barFraction = if (isPostBid) 1f else fraction

    Box(
        modifier = modifier
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
