package com.g2b.bidapp.data.remote.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.g2b.bidapp.data.mapper.toModel
import com.g2b.bidapp.data.remote.api.ScsbidInfoApi
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

// 날짜 윈도우 슬라이딩 방식 — BidNoticePagingSource와 동일 패턴
// key = daysBack: 0 = 오늘, 1 = 어제, ...
// 개찰일(opengDt) 기준으로 조회
class BidResultPagingSource(
    private val api: ScsbidInfoApi,
    private val category: BidCategory,
    private val keyword: String? = null,
) : PagingSource<Int, BidResult>() {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val endDateBase: LocalDate = LocalDate.now()
    private val startDateLimit: LocalDate = endDateBase.minusDays(90)

    override fun getRefreshKey(state: PagingState<Int, BidResult>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BidResult> {
        val daysBack = params.key ?: 0
        val targetDate = endDateBase.minusDays(daysBack.toLong())
        if (targetDate < startDateLimit) {
            return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        }

        val bgnDt = targetDate.format(dateFmt) + "0000"
        val endDt = targetDate.format(dateFmt) + "2359"

        return try {
            val firstResponse = fetchDay(bgnDt = bgnDt, endDt = endDt, pageNo = 1)
            val totalCount = firstResponse.response?.body?.totalCount ?: 0
            val totalPages = ceil(totalCount.toFloat() / NUM_OF_ROWS).toInt().coerceAtLeast(1)

            val allDtos = if (totalPages <= 1) {
                firstResponse.response?.body?.items?.item ?: emptyList()
            } else {
                // totalCount > 999인 날: 나머지 페이지 병렬 fetch
                coroutineScope {
                    val extraDeferreds = (2..totalPages).map { pageNo ->
                        async { fetchDay(bgnDt = bgnDt, endDt = endDt, pageNo = pageNo) }
                    }
                    val firstItems = firstResponse.response?.body?.items?.item ?: emptyList()
                    val extraItems = extraDeferreds.awaitAll()
                        .flatMap { it.response?.body?.items?.item ?: emptyList() }
                    firstItems + extraItems
                }
            }

            val results = allDtos
                .map { it.toModel(category) }
                .sortedByDescending { it.rlOpengDt }

            val nextDaysBack = daysBack + 1
            val nextDate = endDateBase.minusDays(nextDaysBack.toLong())
            val nextKey = if (nextDate < startDateLimit) null else nextDaysBack

            LoadResult.Page(data = results, prevKey = null, nextKey = nextKey)
        } catch (e: Exception) {
            Log.e(TAG, "낙찰결과 로드 실패 (daysBack=$daysBack, date=$targetDate)", e)
            LoadResult.Error(e)
        }
    }

    private suspend fun fetchDay(
        bgnDt: String,
        endDt: String,
        pageNo: Int,
    ) = when (category) {
        BidCategory.CNSTWK -> api.getCnstwkList(
            pageNo = pageNo,
            numOfRows = NUM_OF_ROWS,
            inqryBgnDt = bgnDt,
            inqryEndDt = endDt,
            bidNtceNm = keyword,
        )
        BidCategory.SERVC -> api.getServcList(
            pageNo = pageNo,
            numOfRows = NUM_OF_ROWS,
            inqryBgnDt = bgnDt,
            inqryEndDt = endDt,
            bidNtceNm = keyword,
        )
        BidCategory.THNG -> api.getThngList(
            pageNo = pageNo,
            numOfRows = NUM_OF_ROWS,
            inqryBgnDt = bgnDt,
            inqryEndDt = endDt,
            bidNtceNm = keyword,
        )
    }

    companion object {
        private const val TAG = "BidResultPagingSource"
        const val NUM_OF_ROWS = 999
    }
}
