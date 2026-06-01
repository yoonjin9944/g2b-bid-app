package com.g2b.bidapp.ui.bid.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.di.IoDispatcher
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.repository.WatchlistRepository
import com.g2b.bidapp.domain.usecase.AddToWatchlistUseCase
import com.g2b.bidapp.domain.usecase.RemoveFromWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BidDetailUiState {
    data object Idle : BidDetailUiState
    data class Success(
        val notice: BidNotice,
        val isWatched: Boolean,
    ) : BidDetailUiState
}

@HiltViewModel
class BidDetailViewModel @Inject constructor(
    private val addToWatchlistUseCase: AddToWatchlistUseCase,
    private val removeFromWatchlistUseCase: RemoveFromWatchlistUseCase,
    private val watchlistRepository: WatchlistRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BidDetailUiState>(BidDetailUiState.Idle)
    val uiState: StateFlow<BidDetailUiState> = _uiState.asStateFlow()

    // BidDetailBottomSheet가 열릴 때 호출. Room에서 실제 isWatched 상태를 조회함
    fun setNotice(notice: BidNotice) {
        viewModelScope.launch(ioDispatcher) {
            val isWatched = watchlistRepository.isWatched(notice.bidNtceNo)
            _uiState.update { BidDetailUiState.Success(notice, isWatched = isWatched) }
        }
    }

    fun toggleWatchlist() {
        val current = _uiState.value as? BidDetailUiState.Success ?: return

        // 낙관적 UI 업데이트: 응답 전에 먼저 상태 반전
        _uiState.update { current.copy(isWatched = !current.isWatched) }

        viewModelScope.launch {
            val result = if (current.isWatched) {
                removeFromWatchlistUseCase(current.notice.bidNtceNo)
            } else {
                addToWatchlistUseCase(current.notice)
            }
            // 실패 시 낙관적 업데이트 롤백
            if (result.isFailure) {
                _uiState.update { current }
            }
        }
    }
}