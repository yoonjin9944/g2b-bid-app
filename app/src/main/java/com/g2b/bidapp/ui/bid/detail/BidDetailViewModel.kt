package com.g2b.bidapp.ui.bid.detail

import androidx.lifecycle.ViewModel
import com.g2b.bidapp.domain.model.BidNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface BidDetailUiState {
    data object Idle : BidDetailUiState
    data class Success(
        val notice: BidNotice,
        val isWatched: Boolean,
    ) : BidDetailUiState
}

@HiltViewModel
class BidDetailViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<BidDetailUiState>(BidDetailUiState.Idle)
    val uiState: StateFlow<BidDetailUiState> = _uiState.asStateFlow()

    fun setNotice(notice: BidNotice) {
        _uiState.update {
            BidDetailUiState.Success(notice, isWatched = notice.isWatched)
        }
    }

    fun toggleWatchlist() {
        val current = _uiState.value as? BidDetailUiState.Success ?: return
        _uiState.update { current.copy(isWatched = !current.isWatched) }
        // TODO: phase 6: WatchlistRepository.add / remove 연동 예정
    }
}