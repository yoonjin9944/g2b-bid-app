package com.g2b.bidapp.domain.usecase

import androidx.paging.PagingData
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.domain.repository.BidRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBidNoticeListUseCase @Inject constructor(
    private val bidRepository: BidRepository,
) {
    operator fun invoke(params: SearchParams): Flow<PagingData<BidNotice>> =
        bidRepository.getBidNoticeList(params)
}