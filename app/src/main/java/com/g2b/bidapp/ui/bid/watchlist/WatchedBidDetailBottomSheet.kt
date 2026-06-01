package com.g2b.bidapp.ui.bid.watchlist

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.g2b.bidapp.domain.model.BidStatus
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.ui.components.BidStatusBadge
import com.g2b.bidapp.ui.theme.NavyBlue
import com.g2b.bidapp.util.toDisplayDateTime
import com.g2b.bidapp.util.toPriceLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchedBidDetailBottomSheet(
    bid: WatchedBid,
    onDismiss: () -> Unit,
    onRemoveFromWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = Color(0xFFF8F9FF),
        dragHandle = null,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WatchedBidDetailHeader(
                title = bid.bidNtceNm,
                status = bid,
                onClose = onDismiss,
                onRemoveFromWatchlist = onRemoveFromWatchlist,
            )

            HorizontalDivider(color = Color(0xFFDCE9FF))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DetailSection(title = "공고 상태") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "현재 상태",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF737780),
                            modifier = Modifier.width(76.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        BidStatusBadge(status = bid.currentStatus)
                    }
                }

                DetailSection(title = "공고 기본정보") {
                    DetailRow("공고번호", bid.bidNtceNo)
                    DetailRow("공고기관", bid.ntceInsttNm ?: "-")
                    DetailRow("수요기관", bid.dmInsttNm ?: bid.ntceInsttNm ?: "-")
                    DetailRow("업무분류", bid.bidCategory.label)
                }

                DetailSection(title = "공고 일정") {
                    DetailRow("공고일시", bid.bidNtceDt.toDisplayDateTime())
                    DetailRow("마감일시", bid.bidClseDt.toDisplayDateTime())
                    DetailRow("개찰일시", bid.opengDt.toDisplayDateTime())
                    DetailRow("관심등록일", bid.watchedAt.toDisplayDateTime())
                }

                DetailSection(title = "금액 정보") {
                    DetailRow("추정가격", bid.presmptPrce?.toPriceLabel() ?: "-")
                    DetailRow("예산금액", bid.bdgtAmt?.toPriceLabel() ?: "-")
                }
            }

            WatchedBidDetailBottomBar(
                url = bid.bidNtceDtlUrl,
                onOpenUrl = { url -> context.openUrl(url) },
            )
        }
    }
}

@Composable
private fun WatchedBidDetailHeader(
    title: String,
    status: WatchedBid,
    onClose: () -> Unit,
    onRemoveFromWatchlist: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
            .padding(start = 20.dp, end = 4.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 24.sp,
            ),
            color = Color(0xFF0B1C30),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemoveFromWatchlist) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = "관심공고 해제",
                tint = NavyBlue,
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = Color(0xFF43474F))
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = NavyBlue,
        )
        Spacer(Modifier.size(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF737780),
            modifier = Modifier.width(76.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF0B1C30),
            modifier = Modifier.weight(1f),
        )
    }
    HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 0.5.dp)
}

@Composable
private fun WatchedBidDetailBottomBar(
    url: String?,
    onOpenUrl: (String) -> Unit,
) {
    Surface(
        shadowElevation = 8.dp,
        color = Color(0xFFF8FAFC),
    ) {
        Button(
            onClick = { url?.let { onOpenUrl(it) } },
            enabled = !url.isNullOrBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                Icons.Outlined.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("나라장터에서 보기", color = Color.White)
        }
    }
}

private fun Context.openUrl(url: String) {
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, url.toUri())
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            // 브라우저 없는 환경 — 무시
        }
    }
}

private fun sampleWatchedBid() = WatchedBid(
    id = "sample-id",
    bidNtceNo = "20240001234",
    bidNtceNm = "2024년도 시설물 유지보수 공사 (샘플 공고명)",
    ntceInsttNm = "서울특별시",
    dmInsttNm = "서울특별시 강남구",
    bidNtceDt = 0L,
    bidClseDt = 70L,
    opengDt = null,
    presmptPrce = 250000000L,
    bdgtAmt = null,
    bidCategory = com.g2b.bidapp.domain.model.BidCategory.CNSTWK,
    currentStatus = BidStatus.REGISTERED,
    bidNtceDtlUrl = null,
    watchedAt = System.currentTimeMillis(),
    syncedAt = System.currentTimeMillis(),
)

@Preview(name = "WatchedBidDetailBottomSheet")
@Composable
private fun WatchedBidDetailBottomSheetPreview() {
    WatchedBidDetailBottomSheet(
        bid = sampleWatchedBid(),
        onDismiss = {},
        onRemoveFromWatchlist = {},
    )
}