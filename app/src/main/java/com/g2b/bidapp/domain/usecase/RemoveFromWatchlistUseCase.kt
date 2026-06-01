package com.g2b.bidapp.domain.usecase

import com.g2b.bidapp.domain.repository.WatchlistRepository
import javax.inject.Inject

class RemoveFromWatchlistUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
) {
    suspend operator fun invoke(bidNtceNo: String): Result<Unit> =
        watchlistRepository.removeFromWatchlist(bidNtceNo)
}