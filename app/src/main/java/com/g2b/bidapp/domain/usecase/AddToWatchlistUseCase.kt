package com.g2b.bidapp.domain.usecase

import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.repository.WatchlistRepository
import javax.inject.Inject

class AddToWatchlistUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
) {
    suspend operator fun invoke(notice: BidNotice): Result<Unit> {
        if (watchlistRepository.isWatched(notice.bidNtceNo)) {
            return Result.failure(IllegalStateException("이미 등록된 관심공고입니다"))
        }
        return watchlistRepository.addToWatchlist(notice)
    }
}