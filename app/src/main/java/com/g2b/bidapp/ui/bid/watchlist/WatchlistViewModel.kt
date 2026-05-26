package com.g2b.bidapp.ui.bid.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.di.IoDispatcher
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.domain.repository.WatchlistRepository
import com.g2b.bidapp.domain.usecase.GetWatchlistUseCase
import com.g2b.bidapp.domain.usecase.RemoveFromWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val items: List<WatchedBid> = emptyList(),
    val keyword: String = "",
    val isLoading: Boolean = true,
    val pendingDeleteBid: WatchedBid? = null,
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val getWatchlistUseCase: GetWatchlistUseCase,
    private val removeFromWatchlistUseCase: RemoveFromWatchlistUseCase,
    private val watchlistRepository: WatchlistRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _keyword = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _pendingDeleteBid = MutableStateFlow<WatchedBid?>(null)

    val uiState: StateFlow<WatchlistUiState> = combine(
        _keyword.flatMapLatest { kw -> getWatchlistUseCase(kw) },
        _keyword,
        _isLoading,
        _pendingDeleteBid,
    ) { items, kw, loading, pending ->
        WatchlistUiState(
            items = items,
            keyword = kw,
            isLoading = loading,
            pendingDeleteBid = pending,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WatchlistUiState(),
    )

    init {
        viewModelScope.launch {
            // Room Flow 첫 방출 시 로딩 완료
            getWatchlistUseCase("").collect { _isLoading.value = false }
        }
    }

    fun onKeywordChange(keyword: String) {
        _keyword.update { keyword }
    }

    // 스와이프 삭제: Room + Supabase 즉시 삭제. Snackbar "실행취소"를 위해 bid 보관
    fun deleteItem(bid: WatchedBid) {
        _pendingDeleteBid.update { bid }
        viewModelScope.launch(ioDispatcher) {
            removeFromWatchlistUseCase(bid.bidNtceNo)
        }
    }

    // Snackbar "실행취소" → WatchedBid를 Room에 재삽입 + Supabase upsert 시도
    fun undoDelete(bid: WatchedBid) {
        _pendingDeleteBid.update { null }
        viewModelScope.launch(ioDispatcher) {
            watchlistRepository.restoreWatchedBid(bid)
        }
    }

    // Snackbar가 시간 초과로 닫힘 → pendingDelete만 초기화 (삭제 이미 확정됨)
    fun confirmDelete() {
        _pendingDeleteBid.update { null }
    }
}
