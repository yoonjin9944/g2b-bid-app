package com.g2b.bidapp.ui.bid.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.domain.usecase.GetBidNoticeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BidListUiState(
    val selectedTab: BidCategory = BidCategory.CNSTWK,
    val searchParams: SearchParams = SearchParams(),
)

@HiltViewModel
class BidListViewModel @Inject constructor(
    private val getBidNoticeListUseCase: GetBidNoticeListUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BidListUiState(
            selectedTab = BidCategory.fromApiCode(
                savedStateHandle["category"] ?: BidCategory.CNSTWK.apiCode
            )
        )
    )
    val uiState: StateFlow<BidListUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow: Flow<PagingData<BidNotice>> = _uiState
        .flatMapLatest { state ->
            val params = state.searchParams.copy(category = state.selectedTab)
            getBidNoticeListUseCase(params)
        }
        .cachedIn(viewModelScope)

    fun onTabSelected(category: BidCategory) {
        _uiState.update { it.copy(selectedTab = category) }
    }

    fun applySearchParams(params: SearchParams) {
        _uiState.update { it.copy(searchParams = params) }
    }

    fun clearSearchParams() {
        _uiState.update { it.copy(searchParams = SearchParams()) }
    }
}