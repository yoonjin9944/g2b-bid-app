package com.g2b.bidapp.ui.bid.list

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.domain.repository.AuthRepository
import com.g2b.bidapp.domain.repository.WatchlistRepository
import com.g2b.bidapp.domain.usecase.AddToWatchlistUseCase
import com.g2b.bidapp.domain.usecase.GetBidNoticeListUseCase
import com.g2b.bidapp.domain.usecase.RemoveFromWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BidListUiState(
    val selectedTab: BidCategory = BidCategory.CNSTWK,
    val searchParams: SearchParams = SearchParams(),
)

@HiltViewModel
class BidListViewModel @Inject constructor(
    private val getBidNoticeListUseCase: GetBidNoticeListUseCase,
    private val authRepository: AuthRepository,
    private val watchlistRepository: WatchlistRepository,
    private val addToWatchlistUseCase: AddToWatchlistUseCase,
    private val removeFromWatchlistUseCase: RemoveFromWatchlistUseCase,
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

    val isLoggedIn: StateFlow<Boolean> = flow {
        emit(authRepository.getCurrentUser() != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Room에서 관찰하는 관심공고 번호 집합. BidNoticeCard의 즐겨찾기 아이콘 상태에 사용
    val watchedBidNos: StateFlow<Set<String>> = watchlistRepository
        .getWatchlistFlow()
        .map { list -> list.map { it.bidNtceNo }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

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
        _uiState.update {
            it.copy(
                searchParams = params,
                selectedTab = params.category ?: it.selectedTab,
            )
        }
    }

    fun clearSearchParams() {
        _uiState.update { it.copy(searchParams = SearchParams()) }
    }

    fun toggleWatchlist(notice: BidNotice) {
        viewModelScope.launch {
            val result = if (notice.bidNtceNo in watchedBidNos.value) {
                removeFromWatchlistUseCase(notice.bidNtceNo)
            } else {
                addToWatchlistUseCase(notice)
            }
            result.onFailure { e ->
                Log.e(TAG, "관심공고 토글 실패 (bidNtceNo=${notice.bidNtceNo})", e)
            }
        }
    }

    companion object {
        private const val TAG = "BidListViewModel"
    }
}