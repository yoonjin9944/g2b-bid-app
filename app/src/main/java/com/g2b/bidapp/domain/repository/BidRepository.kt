package com.g2b.bidapp.domain.repository

import androidx.paging.PagingData
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import kotlinx.coroutines.flow.Flow

interface BidRepository {
    fun getBidNoticeList(params: SearchParams): Flow<PagingData<BidNotice>>
}