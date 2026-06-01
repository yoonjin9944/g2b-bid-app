package com.g2b.bidapp.ui.bid.search

import androidx.lifecycle.ViewModel
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.util.toApiQueryDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SearchUiState(
    val keyword: String = "",
    val bidNtceNo: String = "",
    val dmInsttNm: String = "",
    val selectedCategory: BidCategory? = null,
    val fromDateMillis: Long? = null,
    val toDateMillis: Long? = null,
)

val SearchUiState.toParams: SearchParams
    get() = SearchParams(
        keyword = keyword.trim(),
        bidNtceNo = bidNtceNo.trim(),
        dmInsttNm = dmInsttNm.trim(),
        category = selectedCategory,
        inqryBgnDt = fromDateMillis?.toApiQueryDate(endOfDay = false) ?: "",
        inqryEndDt = toDateMillis?.toApiQueryDate(endOfDay = true) ?: "",
    )

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onKeywordChange(value: String) = _uiState.update { it.copy(keyword = value) }
    fun onBidNtceNoChange(value: String) = _uiState.update { it.copy(bidNtceNo = value) }
    fun onDmInsttNmChange(value: String) = _uiState.update { it.copy(dmInsttNm = value) }
    fun onCategorySelect(category: BidCategory?) = _uiState.update { it.copy(selectedCategory = category) }

    fun onDateRangeSelected(fromMillis: Long?, toMillis: Long?) {
        _uiState.update { it.copy(fromDateMillis = fromMillis, toDateMillis = toMillis) }
    }

    fun reset() {
        _uiState.value = SearchUiState()
    }
}

