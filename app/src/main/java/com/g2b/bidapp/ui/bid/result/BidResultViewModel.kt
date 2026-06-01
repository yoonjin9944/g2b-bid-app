package com.g2b.bidapp.ui.bid.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.g2b.bidapp.data.remote.api.ScsbidInfoApi
import com.g2b.bidapp.data.remote.paging.BidResultPagingSource
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class BidResultViewModel @Inject constructor(
    private val scsbidInfoApi: ScsbidInfoApi,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(BidCategory.CNSTWK)
    val selectedCategory: StateFlow<BidCategory> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val bidResults: Flow<PagingData<BidResult>> = _selectedCategory
        .flatMapLatest { category ->
            Pager(
                config = PagingConfig(
                    pageSize = BidResultPagingSource.NUM_OF_ROWS,
                    initialLoadSize = BidResultPagingSource.NUM_OF_ROWS,
                    prefetchDistance = 2,
                ),
                pagingSourceFactory = {
                    BidResultPagingSource(
                        api = scsbidInfoApi,
                        category = category,
                    )
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    fun selectCategory(category: BidCategory) {
        _selectedCategory.value = category
    }
}
