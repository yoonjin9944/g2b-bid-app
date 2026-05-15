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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g2b.bidapp.data.mapper.toPriceLabel
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.ui.theme.BidNoticeColor
import com.g2b.bidapp.ui.theme.NavyBlue
import com.g2b.bidapp.ui.theme.StatusCancelled
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DtFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
private val DtFormatterShort = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

@Composable
fun BidNoticeCard(
    notice: BidNotice,
    onCardClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysRemaining = remember(notice.bidClseDt) { notice.daysRemaining() }
    val statusLabel = remember(daysRemaining) { resolveStatusLabel(daysRemaining) }
    val statusColor = remember(daysRemaining) { resolveStatusColor(daysRemaining) }
    val statusTextColor = remember(daysRemaining) { resolveStatusTextColor(daysRemaining) }

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
                        label = statusLabel,
                        containerColor = statusColor,
                        contentColor = statusTextColor,
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
                        imageVector = if (notice.isWatched) Icons.Filled.Bookmark
                        else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (notice.isWatched) "관심공고 해제" else "관심공고 추가",
                        tint = if (notice.isWatched) NavyBlue else Color(0xFF94A3B8),
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
                    text = "마감일시: ${notice.bidClseDt?.toDisplayDate() ?: "-"}",
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
                    DeadlineProgressBar(daysRemaining = daysRemaining)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            daysRemaining == null -> ""
                            daysRemaining < 0 -> "마감됨"
                            else -> "D-$daysRemaining"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                        ),
                        color = daysUrgencyColor(daysRemaining),
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
private fun DeadlineProgressBar(daysRemaining: Int?) {
    val fraction = when {
        daysRemaining == null || daysRemaining < 0 -> 1f
        daysRemaining == 0 -> 1f
        daysRemaining >= 30 -> 0f
        else -> 1f - (daysRemaining / 30f)
    }
    val barColor = daysUrgencyColor(daysRemaining)

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(8.dp)
            .clip(CircleShape)
            .background(Color(0xFFDCE9FF)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(CircleShape)
                .background(barColor),
        )
    }
}

private fun resolveStatusLabel(daysRemaining: Int?): String = when {
    daysRemaining == null -> "공고중"
    daysRemaining < 0 -> "마감"
    daysRemaining <= 3 -> "마감임박"
    else -> "공고중"
}

private fun resolveStatusColor(daysRemaining: Int?): Color = when {
    daysRemaining == null -> Color(0xFF001E40)
    daysRemaining < 0 -> Color(0xFFDCE9FF)
    daysRemaining <= 3 -> Color(0xFFFFDAD6)
    else -> Color(0xFF001E40)
}

private fun resolveStatusTextColor(daysRemaining: Int?): Color = when {
    daysRemaining == null -> Color(0xFF799DD6)
    daysRemaining < 0 -> BidNoticeColor
    daysRemaining <= 3 -> StatusCancelled
    else -> Color(0xFF799DD6)
}

private fun daysUrgencyColor(daysRemaining: Int?): Color = when {
    daysRemaining == null || daysRemaining > 7 -> Color(0xFF0060AC)
    daysRemaining <= 3 -> StatusCancelled
    else -> Color(0xFF0060AC)
}

private fun BidNotice.daysRemaining(): Int? {
    val raw = bidClseDt ?: return null
    return try {
        val formatter = if (raw.length >= 14) DtFormatter else DtFormatterShort
        val clse = LocalDateTime.parse(raw.take(if (raw.length >= 14) 14 else 12), formatter)
        ChronoUnit.DAYS.between(LocalDateTime.now(), clse).toInt()
    } catch (_: Exception) {
        null
    }
}

private fun String.toDisplayDate(): String = try {
    val src = if (length >= 14) DtFormatter else DtFormatterShort
    val dt = LocalDateTime.parse(take(if (length >= 14) 14 else 12), src)
    dt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
} catch (_: Exception) {
    this
}