package com.g2b.bidapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.g2b.bidapp.data.remote.api.BidPublicInfoApi
import com.g2b.bidapp.data.remote.paging.BidNoticePagingSource
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.domain.repository.BidRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BidRepositoryImpl @Inject constructor(
    private val api: BidPublicInfoApi,
) : BidRepository {

    override fun getBidNoticeList(params: SearchParams): Flow<PagingData<BidNotice>> =
        Pager(
            config = PagingConfig(
                pageSize = BidNoticePagingSource.NUM_OF_ROWS,
                initialLoadSize = BidNoticePagingSource.NUM_OF_ROWS,
                prefetchDistance = 2,
            ),
            pagingSourceFactory = { BidNoticePagingSource(api, params) },
        ).flow
}