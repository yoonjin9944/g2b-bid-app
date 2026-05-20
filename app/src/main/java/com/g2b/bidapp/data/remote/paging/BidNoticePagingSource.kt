package com.g2b.bidapp.data.remote.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.g2b.bidapp.data.mapper.toModel
import com.g2b.bidapp.data.remote.api.BidPublicInfoApi
import com.g2b.bidapp.data.remote.dto.BidNoticeListResponse
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
            Log.e(TAG, "페이지 로드 실패 (page=$pageNo, category=${this@BidNoticePagingSource.params.category})", e)
            LoadResult.Error(e)
        }
    }

    private fun defaultDateRange(): Pair<String, String> {
        val fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        val now = LocalDateTime.now()
        return now.minusDays(30).format(fmt) to now.format(fmt)
    }

    private suspend fun fetchPage(pageNo: Int): BidNoticeListResponse {
        val (defaultBgn, defaultEnd) = defaultDateRange()
        val bgnDt = params.inqryBgnDt.ifBlank { defaultBgn }
        val endDt = params.inqryEndDt.ifBlank { defaultEnd }

        return when (params.category) {
            BidCategory.SERVC -> api.getServcList(
                pageNo = pageNo,
                numOfRows = NUM_OF_ROWS,
                bidNtceNm = params.keyword.ifBlank { null },
                bidNtceNo = params.bidNtceNo.ifBlank { null },
                dmInsttNm = params.dmInsttNm.ifBlank { null },
                inqryBgnDt = bgnDt,
                inqryEndDt = endDt,
            )

            BidCategory.THNG -> api.getThngList(
                pageNo = pageNo,
                numOfRows = NUM_OF_ROWS,
                bidNtceNm = params.keyword.ifBlank { null },
                bidNtceNo = params.bidNtceNo.ifBlank { null },
                dmInsttNm = params.dmInsttNm.ifBlank { null },
                inqryBgnDt = bgnDt,
                inqryEndDt = endDt,
            )

            else -> api.getCnstwkList(
                pageNo = pageNo,
                numOfRows = NUM_OF_ROWS,
                bidNtceNm = params.keyword.ifBlank { null },
                bidNtceNo = params.bidNtceNo.ifBlank { null },
                dmInsttNm = params.dmInsttNm.ifBlank { null },
                inqryBgnDt = bgnDt,
                inqryEndDt = endDt,
            )
        }
    }

    companion object {
        private const val TAG = "BidNoticePagingSource"
        const val NUM_OF_ROWS = 20
    }

}