package com.g2b.bidapp.domain.usecase

import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWatchlistUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
) {
    operator fun invoke(keyword: String = ""): Flow<List<WatchedBid>> =
        if (keyword.isBlank()) watchlistRepository.getWatchlistFlow()
        else watchlistRepository.getWatchlistByKeywordFlow(keyword)
}