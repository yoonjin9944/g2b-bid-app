package com.g2b.bidapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import com.g2b.bidapp.domain.model.BidStatus
import com.g2b.bidapp.ui.theme.StatusCancelled
import com.g2b.bidapp.ui.theme.StatusChanged
import com.g2b.bidapp.ui.theme.StatusOpened
import com.g2b.bidapp.ui.theme.StatusReopened

/**
 * 관심공고 상태 배지 컴포넌트.
 * BidStatus 값에 따라 브랜드 색상 팔레트로 표시됩니다.
 */
@Composable
fun BidStatusBadge(
    status: BidStatus,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor, label) = when (status) {
        BidStatus.REGISTERED -> Triple(
            Color(0xFFDCE9FF),
            Color(0xFF001E40),
            "등록",
        )
        BidStatus.CHANGED -> Triple(
            Color(0xFFFFE0CC),
            StatusChanged,
            "변경",
        )
        BidStatus.CANCELLED -> Triple(
            Color(0xFFFFDAD6),
            StatusCancelled,
            "취소",
        )
        BidStatus.REOPENED -> Triple(
            Color(0xFFD6E4FF),
            StatusReopened,
            "재공고",
        )
        BidStatus.OPENED -> Triple(
            Color(0xFFE8EAED),
            StatusOpened,
            "개찰",
        )
    }

    Box(
        modifier = modifier
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
