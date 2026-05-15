package com.g2b.bidapp.ui.bid.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.ui.theme.BorderGray
import com.g2b.bidapp.ui.theme.NavyBlue

private val categoryChips = listOf(
    null to "전체",
    BidCategory.CNSTWK to "공사",
    BidCategory.SERVC to "용역",
    BidCategory.THNG to "물품",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onSearch: (SearchParams) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "나라장터 모니터링",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = NavyBlue,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "뒤로", tint = NavyBlue)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "알림",
                            tint = Color(0xFF94A3B8),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC)),
            )
        },
        containerColor = Color(0xFFF8F9FF),
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "통합검색",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF0B1C30),
                )
            }

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, BorderGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
            ) {
                SearchLabel("공고명")
                Spacer(Modifier.height(8.dp))
                SearchTextField(
                    value = state.keyword,
                    onValueChange = viewModel::onKeywordChange,
                    placeholder = "검색어 또는 공고명을 입력하세요",
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    },
                )

                Spacer(Modifier.height(16.dp))

                SearchLabel("공고번호")
                Spacer(Modifier.height(8.dp))
                SearchTextField(
                    value = state.bidNtceNo,
                    onValueChange = viewModel::onBidNtceNoChange,
                    placeholder = "예: 20231012345-00",
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = BorderGray.copy(alpha = 0.3f))
                Spacer(Modifier.height(24.dp))

                Text(
                    "상세 필터",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF0B1C30),
                )

                Spacer(Modifier.height(16.dp))

                FilterLabel("수요기관")
                Spacer(Modifier.height(8.dp))
                SearchTextField(
                    value = state.dmInsttNm,
                    onValueChange = viewModel::onDmInsttNmChange,
                    placeholder = "수요기관명 입력",
                    leadingIcon = {
                        Icon(Icons.Outlined.Business, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    },
                )

                Spacer(Modifier.height(16.dp))

                FilterLabel("업무종류")
                Spacer(Modifier.height(8.dp))
                CategoryChipGroup(
                    selected = state.selectedCategory,
                    onSelect = viewModel::onCategorySelect,
                )

                Spacer(Modifier.height(16.dp))

                FilterLabel("공고일자")
                Spacer(Modifier.height(8.dp))
                DateRangeRow(
                    dateFrom = state.dateFrom,
                    dateTo = state.dateTo,
                    onDateFromChange = viewModel::onDateFromChange,
                    onDateToChange = viewModel::onDateToChange,
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::reset,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF43474F)),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("초기화")
                    }
                    Button(
                        onClick = {
                            onSearch(state.toParams)
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    ) {
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("검색하기", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SearchLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
        color = Color(0xFF0B1C30),
    )
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF43474F),
    )
}

@Composable
private fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                placeholder,
                color = Color(0xFF737780).copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = leadingIcon,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFF8F9FF),
            focusedContainerColor = Color(0xFFF8F9FF),
            unfocusedBorderColor = BorderGray,
            focusedBorderColor = NavyBlue,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipGroup(
    selected: BidCategory?,
    onSelect: (BidCategory?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categoryChips.forEach { (category, label) ->
            val isSelected = category == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) NavyBlue else Color(0xFFF8F9FF))
                    .border(1.dp, if (isSelected) Color.Transparent else BorderGray, CircleShape)
                    .clickable { onSelect(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) Color(0xFF799DD6) else Color(0xFF43474F),
                )
            }
        }
    }
}

@Composable
private fun DateRangeRow(
    dateFrom: String,
    dateTo: String,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = dateFrom,
            onValueChange = onDateFromChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("시작일 (yyyyMMdd)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF8F9FF),
                focusedContainerColor = Color(0xFFF8F9FF),
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = NavyBlue,
            ),
        )

        Text("~", color = BorderGray)

        OutlinedTextField(
            value = dateTo,
            onValueChange = onDateToChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("종료일 (yyyyMMdd)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF8F9FF),
                focusedContainerColor = Color(0xFFF8F9FF),
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = NavyBlue,
            ),
        )
    }
}