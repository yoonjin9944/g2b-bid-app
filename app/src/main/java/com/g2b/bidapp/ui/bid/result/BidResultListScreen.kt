package com.g2b.bidapp.ui.bid.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidResult
import com.g2b.bidapp.ui.components.EmptyView
import com.g2b.bidapp.ui.components.ErrorView
import com.g2b.bidapp.ui.theme.NavyBlue
import com.g2b.bidapp.util.toDisplayDateTime
import com.g2b.bidapp.util.toPriceLabel

private val tabCategories = listOf(
    BidCategory.CNSTWK to "공사",
    BidCategory.SERVC to "용역",
    BidCategory.THNG to "물품",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidResultListScreen(
    modifier: Modifier = Modifier,
    viewModel: BidResultViewModel = hiltViewModel(),
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val bidResults = viewModel.bidResults.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "낙찰결과",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                        ),
                        color = NavyBlue,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC)),
            )
        },
        containerColor = Color(0xFFF8F9FF),
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            BidResultTabRow(
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::selectCategory,
            )

            BidResultContent(
                pagingItems = bidResults,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BidResultTabRow(
    selectedCategory: BidCategory,
    onCategorySelected: (BidCategory) -> Unit,
) {
    val selectedIndex = tabCategories.indexOfFirst { it.first == selectedCategory }.coerceAtLeast(0)

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color(0xFFF8FAFC),
        contentColor = NavyBlue,
        indicator = { tabPositions ->
            androidx.compose.material3.TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = NavyBlue,
            )
        },
    ) {
        tabCategories.forEachIndexed { index, (category, label) ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (index == selectedIndex) NavyBlue else Color(0xFF94A3B8),
                    )
                },
            )
        }
    }
}

@Composable
private fun BidResultContent(
    pagingItems: LazyPagingItems<BidResult>,
    modifier: Modifier = Modifier,
) {
    when {
        pagingItems.loadState.refresh is LoadState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyBlue)
            }
        }

        pagingItems.loadState.refresh is LoadState.Error -> {
            val error = (pagingItems.loadState.refresh as LoadState.Error).error
            ErrorView(
                message = error.localizedMessage ?: "낙찰결과를 불러오지 못했습니다",
                onRetry = { pagingItems.retry() },
                modifier = modifier,
            )
        }

        pagingItems.itemCount == 0 && pagingItems.loadState.append.endOfPaginationReached -> {
            EmptyView(
                message = "낙찰결과가 없습니다",
                subMessage = "최근 90일 이내의 낙찰결과가 표시됩니다",
                modifier = modifier,
            )
        }

        else -> {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = modifier,
            ) {
                items(pagingItems.itemCount) { index ->
                    val result = pagingItems[index] ?: return@items
                    BidResultCard(result = result)
                }

                when (pagingItems.loadState.append) {
                    is LoadState.Loading -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = NavyBlue, modifier = Modifier.height(24.dp))
                        }
                    }
                    is LoadState.Error -> item {
                        ErrorView(
                            message = "추가 데이터를 불러오지 못했습니다",
                            onRetry = { pagingItems.retry() },
                        )
                    }
                    else -> Unit
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun BidResultCard(
    result: BidResult,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "공고번호: ${result.bidNtceNo}-${result.bidNtceOrd}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF737780),
                )
                Text(
                    text = result.rlOpengDt?.toDisplayDateTime() ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF737780),
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = result.bidNtceNm,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                ),
                color = Color(0xFF0B1C30),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = result.dminsttNm ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFDCE9FF))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "낙찰업체",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                    )
                    Text(
                        text = result.bidwinnrNm ?: "-",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF0B1C30),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "낙찰금액",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                    )
                    Text(
                        text = result.sucsfbidAmt?.toPriceLabel() ?: "-",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = NavyBlue,
                        ),
                    )
                }
            }

            if (result.sucsfbidRate != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "낙찰율: ${result.sucsfbidRate}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

private fun sampleResult() = BidResult(
    bidNtceNo = "sampleNo",
    bidNtceOrd = "sampleOrd",
    bidNtceNm = "smapleBidNtceNm",
    ntceInsttNm = "sampleNtceInsttNm",
    dminsttNm = "dminsttNm",
    rlOpengDt = "",       // 개찰일시 (yyyyMMddHHmm)
    bidwinnrNm = "",      // 낙찰업체명
    sucsfbidAmt = null,       // 낙찰금액 (원)
    presmptPrce = null,     // 추정가격 (원)
    bdgtAmt = null,         // 예산금액 (원)
    sucsfbidRate = null,  // 낙찰율 (%)
    bidCategory = BidCategory.CNSTWK,
    bidNtceDtlUrl = null,
)

@Composable
@Preview(showBackground = true)
private fun BidResultCardPreview() {
    BidResultCard(
        result = sampleResult(),
        modifier = Modifier.fillMaxWidth(),
    )

}