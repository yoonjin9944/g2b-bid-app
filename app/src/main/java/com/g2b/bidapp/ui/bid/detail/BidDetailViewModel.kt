package com.g2b.bidapp.ui.bid.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g2b.bidapp.data.local.dao.WatchedBidDao
import com.g2b.bidapp.data.mapper.toModel
import com.g2b.bidapp.di.IoDispatcher
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.domain.repository.AuthRepository
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed interface BidDetailUiState {
    data object Idle : BidDetailUiState
    data object Loading : BidDetailUiState
    data object NotFound : BidDetailUiState  // FCM 진입 시 Room에 데이터 없는 경우
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
    private val watchedBidDao: WatchedBidDao,
    private val authRepository: AuthRepository,
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

    // FCM 딥링크 진입 시 bidNtceNo만 있는 경우 — Room(관심공고 DB)에서 조회
    fun loadByBidNtceNo(bidNtceNo: String) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { BidDetailUiState.Loading }
            val userId = authRepository.getCurrentUser()?.id
            val entity = userId?.let { watchedBidDao.getByBidNtceNo(it, bidNtceNo) }

            if (entity == null) {
                // 관심공고에 없는 경우 (비정상 경로) — NotFound 처리
                _uiState.update { BidDetailUiState.NotFound }
                return@launch
            }

            val watchedBid: WatchedBid = entity.toModel()
            val notice = BidNotice(
                bidNtceNo = watchedBid.bidNtceNo,
                bidNtceOrd = "00",
                bidNtceNm = watchedBid.bidNtceNm,
                ntceInsttNm = watchedBid.ntceInsttNm,
                dmInsttNm = watchedBid.dmInsttNm,
                bidNtceDt = watchedBid.bidNtceDt?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
                },
                bidClseDt = watchedBid.bidClseDt?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
                },
                opengDt = watchedBid.opengDt?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
                },
                presmptPrce = watchedBid.presmptPrce,
                bdgtAmt = watchedBid.bdgtAmt,
                ntceKindNm = watchedBid.currentStatus.name,
                bidNtceDtlUrl = watchedBid.bidNtceDtlUrl,
                bidCategory = watchedBid.bidCategory,
                isWatched = true,
            )
            _uiState.update { BidDetailUiState.Success(notice, isWatched = true) }
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