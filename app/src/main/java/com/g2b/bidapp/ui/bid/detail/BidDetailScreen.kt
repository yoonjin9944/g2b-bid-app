package com.g2b.bidapp.ui.bid.detail

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
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.g2b.bidapp.data.mapper.toPriceLabel
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.ui.theme.NavyBlue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DtFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
private val DtFormatterShort = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
private val DisplayFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidDetailBottomSheet(
    notice: BidNotice,
    onDismiss: () -> Unit,
    isLoggedIn: Boolean = true,
    // BidListViewModel의 watchedBidNos에서 미리 알고 있는 상태를 전달받아 순간적인 깜빡임 방지
    initialIsWatched: Boolean = notice.isWatched,
    modifier: Modifier = Modifier,
    viewModel: BidDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(notice.bidNtceNo) {
        viewModel.setNotice(notice)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // ViewModel이 Room 조회를 완료하면 Success 상태의 isWatched를 사용,
    // 아직 Idle이면 부모가 전달한 initialIsWatched 사용
    val isWatched = (uiState as? BidDetailUiState.Success)?.isWatched ?: initialIsWatched

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = Color(0xFFF8F9FF),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            BidDetailHeader(
                title = notice.bidNtceNm,
                isWatched = isWatched,
                isLoggedIn = isLoggedIn,
                onClose = onDismiss,
                onToggleWatchlist = viewModel::toggleWatchlist,
            )

            HorizontalDivider(color = Color(0xFFDCE9FF))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DetailSection(title = "공고 기본정보") {
                    DetailRow("공고번호", "${notice.bidNtceNo}-${notice.bidNtceOrd}")
                    DetailRow("공고기관", notice.ntceInsttNm ?: "-")
                    DetailRow("수요기관", notice.dmInsttNm ?: notice.ntceInsttNm ?: "-")
                    DetailRow("업무분류", notice.bidCategory.label)
                }

                DetailSection(title = "공고 일정") {
                    DetailRow("공고일시", notice.bidNtceDt?.toDisplayDate() ?: "-")
                    DetailRow("마감일시", notice.bidClseDt?.toDisplayDate() ?: "-")
                    DetailRow("개찰일시", notice.opengDt?.toDisplayDate() ?: "-")
                }

                DetailSection(title = "금액 정보") {
                    DetailRow("추정가격", notice.presmptPrce?.toPriceLabel() ?: "-")
                    DetailRow("예산금액", notice.bdgtAmt?.toPriceLabel() ?: "-")
                }
            }

            BidDetailBottomBar(
                url = notice.bidNtceDtlUrl,
                onOpenUrl = { url -> context.openUrl(url) },
            )
        }
    }
}

@Composable
private fun BidDetailHeader(
    title: String,
    isWatched: Boolean,
    isLoggedIn: Boolean,
    onClose: () -> Unit,
    onToggleWatchlist: () -> Unit,
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
        if (isLoggedIn) {
            IconButton(onClick = onToggleWatchlist) {
                Icon(
                    imageVector = if (isWatched) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isWatched) "관심공고 해제" else "관심공고 등록",
                    tint = if (isWatched) NavyBlue else Color(0xFF94A3B8),
                )
            }
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
private fun BidDetailBottomBar(
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
            // 브라우저가 없는 환경 — 무시
        }
    }
}

private fun String.toDisplayDate(): String = try {
    val formatter = if (length >= 14) DtFormatter else DtFormatterShort
    val chars = if (length >= 14) 14 else 12
    LocalDateTime.parse(take(chars), formatter).format(DisplayFormatter)
} catch (_: Exception) {
    this
}