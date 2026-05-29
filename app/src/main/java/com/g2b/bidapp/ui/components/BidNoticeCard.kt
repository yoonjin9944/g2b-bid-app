package com.g2b.bidapp.ui.components

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.ui.theme.BidNoticeColor
import com.g2b.bidapp.ui.theme.NavyBlue
import com.g2b.bidapp.ui.theme.StatusCancelled
import com.g2b.bidapp.util.parseG2bDateTime
import com.g2b.bidapp.util.toDisplayDateTime
import com.g2b.bidapp.util.toPriceLabel
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private enum class BidLifecycleStage {
    BIDDING,        // 투찰 진행 중 (now < bidClseDt)
    BIDDING_URGENT, // 마감임박 (3일 이하)
    AWAITING_OPEN,  // 개찰 대기 (bidClseDt <= now < opengDt)
    OPENED,         // 개찰일 경과
    UNKNOWN,        // 날짜 정보 없음
}

@Composable
fun BidNoticeCard(
    notice: BidNotice,
    onCardClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
    // 페이징 아이템의 notice.isWatched는 항상 false이므로 Room에서 관찰한 값을 외부에서 주입받음
    isWatched: Boolean = notice.isWatched,
    modifier: Modifier = Modifier,
) {
    val stage = remember(notice.bidNtceDt, notice.bidClseDt, notice.opengDt) {
        notice.resolveLifecycleStage()
    }
    val daysRemaining = remember(notice.bidClseDt) { notice.daysRemaining() }
    val lifetimeFraction = remember(notice.bidNtceDt, notice.bidClseDt) {
        notice.lifetimeFraction()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        label = stage.label(),
                        containerColor = stage.containerColor(),
                        contentColor = stage.contentColor(),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "공고번호: ${notice.bidNtceNo}-${notice.bidNtceOrd}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF737780),
                        maxLines = 1,
                    )
                }
                IconButton(onClick = onWatchlistToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isWatched) Icons.Filled.Bookmark
                        else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isWatched) "관심공고 해제" else "관심공고 추가",
                        tint = if (isWatched) NavyBlue else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = notice.bidNtceNm,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                ),
                color = Color(0xFF0B1C30),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp),
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
                    text = "수요기관: ${notice.dmInsttNm ?: notice.ntceInsttNm ?: "-"}",
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
                    text = "마감일시: ${notice.bidClseDt?.toDisplayDateTime() ?: "-"}",
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
                    text = notice.presmptPrce?.toPriceLabel() ?: "-",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF0B1C30),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.6.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = contentColor,
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

// 날짜 세 필드(bidNtceDt, bidClseDt, opengDt)를 조합해 현재 라이프사이클 단계를 판단
private fun BidNotice.resolveLifecycleStage(): BidLifecycleStage {
    val now = LocalDateTime.now()
    val clse = bidClseDt?.parseG2bDateTime() ?: return BidLifecycleStage.UNKNOWN
    val openg = opengDt?.parseG2bDateTime()
    return when {
        now < clse -> {
            val days = ChronoUnit.DAYS.between(now, clse).toInt()
            if (days <= 3) BidLifecycleStage.BIDDING_URGENT else BidLifecycleStage.BIDDING
        }
        openg != null && now < openg -> BidLifecycleStage.AWAITING_OPEN
        openg != null -> BidLifecycleStage.OPENED
        else -> BidLifecycleStage.AWAITING_OPEN  // opengDt 없으면 개찰 대기로 표시
    }
}

// 공고일(bidNtceDt) → 마감일(bidClseDt) 구간 기준 경과 비율 (0f ~ 1f)
// bidNtceDt가 없으면(목록 API 미포함) 마감까지 남은 일수를 60일 기준으로 역산
private fun BidNotice.lifetimeFraction(): Float {
    val end = bidClseDt?.parseG2bDateTime() ?: return 0f
    val now = LocalDateTime.now()
    if (now >= end) return 1f
    val start = bidNtceDt?.parseG2bDateTime()
    return if (start != null && start < end) {
        val total = ChronoUnit.SECONDS.between(start, end).toFloat()
        (ChronoUnit.SECONDS.between(start, now).toFloat() / total).coerceIn(0f, 1f)
    } else {
        val daysLeft = ChronoUnit.DAYS.between(now, end).toFloat().coerceAtLeast(0f)
        (1f - (daysLeft / 60f)).coerceIn(0f, 1f)
    }
}

private fun BidNotice.daysRemaining(): Int? {
    val clse = bidClseDt?.parseG2bDateTime() ?: return null
    return ChronoUnit.DAYS.between(LocalDateTime.now(), clse).toInt()
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

private fun sampleBidNotice() = BidNotice(
    bidNtceNo = "20240001234",
    bidNtceOrd = "000",
    bidNtceNm = "2024년도 시설물 유지보수 공사",
    ntceInsttNm = "서울특별시",
    dmInsttNm = "서울특별시 강남구",
    bidNtceDt = "202405010900",
    bidClseDt = "202406300900",
    opengDt = null,
    presmptPrce = 250000000L,
    bdgtAmt = null,
    bidCategory = BidCategory.CNSTWK,
    bidNtceDtlUrl = null,
    isWatched = false,
)

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FF, name = "BidNoticeCard - 기본")
@Composable
private fun BidNoticeCardPreview() {
    BidNoticeCard(
        notice = sampleBidNotice(),
        isWatched = false,
        onCardClick = {},
        onWatchlistToggle = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FF, name = "BidNoticeCard - 관심공고 등록")
@Composable
private fun BidNoticeCardWatchedPreview() {
    BidNoticeCard(
        notice = sampleBidNotice(),
        isWatched = true,
        onCardClick = {},
        onWatchlistToggle = {},
    )
}