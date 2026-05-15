package com.g2b.bidapp.data.remote.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.g2b.bidapp.data.mapper.toModel
import com.g2b.bidapp.data.remote.api.BidPublicInfoApi
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams

class BidNoticePagingSource(
    private val api: BidPublicInfoApi,
    private val params: SearchParams,
) : PagingSource<Int, BidNotice>() {

    override fun getRefreshKey(state: PagingState<Int, BidNotice>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BidNotice> {
        val pageNo = params.key ?: 1
        return try {
            val response = fetchPage(pageNo)
            val body = response.response?.body
            val totalCount = body?.totalCount ?: 0
            val items = body?.items?.item ?: emptyList()
            val notices = items.map { it.toModel(this@BidNoticePagingSource.params.category ?: BidCategory.CNSTWK) }
            val nextKey = if (pageNo * NUM_OF_ROWS >= totalCount) null else pageNo + 1

            LoadResult.Page(
                data = notices,
                prevKey = null,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun fetchPage(pageNo: Int) = when (params.category) {
        BidCategory.SERVC -> api.getServcList(
            pageNo = pageNo,
            numOfRows = NUM_OF_ROWS,
            bidNtceNm = params.keyword.ifBlank { null },
            bidNtceNo = params.bidNtceNo.ifBlank { null },
            dmInsttNm = params.dmInsttNm.ifBlank { null },
            inqryBgnDt = params.inqryBgnDt.ifBlank { null },
            inqryEndDt = params.inqryEndDt.ifBlank { null },
        )

        BidCategory.THNG -> api.getThngList(
            pageNo = pageNo,
            numOfRows = NUM_OF_ROWS,
            bidNtceNm = params.keyword.ifBlank { null },
            bidNtceNo = params.bidNtceNo.ifBlank { null },
            dmInsttNm = params.dmInsttNm.ifBlank { null },
            inqryBgnDt = params.inqryBgnDt.ifBlank { null },
            inqryEndDt = params.inqryEndDt.ifBlank { null },
        )

        else -> api.getCnstwkList(
            pageNo = pageNo,
            numOfRows = NUM_OF_ROWS,
            bidNtceNm = params.keyword.ifBlank { null },
            bidNtceNo = params.bidNtceNo.ifBlank { null },
            dmInsttNm = params.dmInsttNm.ifBlank { null },
            inqryBgnDt = params.inqryBgnDt.ifBlank { null },
            inqryEndDt = params.inqryEndDt.ifBlank { null },
        )
    }

    companion object {
        const val NUM_OF_ROWS = 20
    }

}