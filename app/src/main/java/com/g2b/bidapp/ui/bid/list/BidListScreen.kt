package com.g2b.bidapp.ui.bid.list

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.ui.components.BidNoticeCard
import com.g2b.bidapp.ui.components.EmptyView
import com.g2b.bidapp.ui.components.ErrorView
import com.g2b.bidapp.ui.theme.NavyBlue

private val tabs = listOf(BidCategory.CNSTWK, BidCategory.SERVC, BidCategory.THNG)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidListScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetail: (bidNtceNo: String) -> Unit,
    incomingSearchParams: SearchParams?,
    modifier: Modifier = Modifier,
    viewModel: BidListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()

    LaunchedEffect(incomingSearchParams) {
        incomingSearchParams?.let { viewModel.applySearchParams(it) }
    }

    Scaffold(
        topBar = {
            BidListTopAppBar(
                hasActiveFilter = !uiState.searchParams.isEmpty,
                onSearchClick = onNavigateToSearch,
            )
        },
        containerColor = Color(0xFFF8F9FF),
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CategoryTabRow(
                selected = uiState.selectedTab,
                onTabSelected = viewModel::onTabSelected,
            )

            if (!uiState.searchParams.isEmpty) {
                ActiveFilterBanner(
                    params = uiState.searchParams,
                    onClear = viewModel::clearSearchParams,
                )
            }

            BidListContent(
                pagingItems = pagingItems,
                onCardClick = onNavigateToDetail,
                onRetry = { pagingItems.retry() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BidListTopAppBar(
    hasActiveFilter: Boolean,
    onSearchClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "나라장터 모니터링",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                ),
                color = NavyBlue,
            )
        },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Menu, contentDescription = "메뉴", tint = NavyBlue)
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "검색",
                    tint = if (hasActiveFilter) NavyBlue else Color(0xFF94A3B8),
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Notifications, contentDescription = "알림", tint = Color(0xFF94A3B8))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFF8FAFC),
        ),
    )
}

@Composable
private fun CategoryTabRow(
    selected: BidCategory,
    onTabSelected: (BidCategory) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selected),
        containerColor = Color(0xFFF8FAFC),
        contentColor = NavyBlue,
        indicator = { tabPositions ->
            val index = tabs.indexOf(selected)
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                color = NavyBlue,
            )
        },
        edgePadding = 0.dp,
    ) {
        tabs.forEachIndexed { index, category ->
            Tab(
                selected = category == selected,
                onClick = { onTabSelected(category) },
                text = {
                    Text(
                        text = category.label,
                        fontWeight = if (category == selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                selectedContentColor = NavyBlue,
                unselectedContentColor = Color(0xFF94A3B8),
            )
        }
    }
}

@Composable
private fun ActiveFilterBanner(
    params: SearchParams,
    onClear: () -> Unit,
) {
    val summary = buildList {
        if (params.keyword.isNotBlank()) add("\"${params.keyword}\"")
        if (params.dmInsttNm.isNotBlank()) add(params.dmInsttNm)
        if (params.inqryBgnDt.isNotBlank()) add("${params.inqryBgnDt} ~")
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE5EEFF))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (summary.isNotBlank()) "필터: $summary" else "필터 적용 중",
            style = MaterialTheme.typography.bodySmall,
            color = NavyBlue,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
            Text("✕", fontSize = 12.sp, color = NavyBlue)
        }
    }
}

@Composable
private fun BidListContent(
    pagingItems: LazyPagingItems<BidNotice>,
    onCardClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        pagingItems.loadState.refresh is LoadState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyBlue)
            }
        }

        pagingItems.loadState.refresh is LoadState.Error -> {
            val error = (pagingItems.loadState.refresh as LoadState.Error).error
            ErrorView(
                message = error.localizedMessage ?: "목록을 불러오지 못했습니다.",
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
        }

        pagingItems.itemCount == 0 && pagingItems.loadState.append.endOfPaginationReached -> {
            EmptyView(modifier = Modifier.fillMaxSize())
        }

        else -> {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.bidNtceNo + it.bidNtceOrd },
                ) { index ->
                    pagingItems[index]?.let { notice ->
                        BidNoticeCard(
                            notice = notice,
                            onCardClick = { onCardClick(notice.bidNtceNo) },
                            onWatchlistToggle = {},
                        )
                    }
                }

                when (pagingItems.loadState.append) {
                    is LoadState.Loading -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = NavyBlue,
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    is LoadState.Error -> item {
                        val err = (pagingItems.loadState.append as LoadState.Error).error
                        ErrorView(
                            message = err.localizedMessage ?: "추가 로딩 실패",
                            onRetry = { pagingItems.retry() },
                        )
                    }

                    else -> item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}