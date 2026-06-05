package com.g2b.bidapp.data.remote.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.g2b.bidapp.data.mapper.toModel
import com.g2b.bidapp.data.remote.api.BidPublicInfoApi
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 날짜 역방향 슬라이딩 페이지 전략 — 다중 날짜 병렬 fetch
 *
 * key(weekBack) = 0 → 최근 DAYS_PER_PAGE일, 1 → 그 이전 DAYS_PER_PAGE일 …
 * 1페이지 로드 시 DAYS_PER_PAGE일치를 병렬 fetch → 동일 대기시간에 더 많은 데이터 수집
 *
 * 누락 방지:
 *   - numOfRows = 999 로 1회 호출에 최대한 많이 수집
 *   - totalCount > 999 시 나머지 페이지를 병렬 fetch 후 합산
 */
class BidNoticePagingSource(
    private val api: BidPublicInfoApi,
    private val params: SearchParams,
) : PagingSource<Int, BidNotice>() {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")

    private val endDateBase: LocalDate = params.inqryEndDt
        .take(8).takeIf { it.length == 8 }
        ?.let { runCatching { LocalDate.parse(it, dateFmt) }.getOrNull() }
        ?: LocalDate.now()

    private val startDateLimit: LocalDate = params.inqryBgnDt
        .take(8).takeIf { it.length == 8 }
        ?.let { runCatching { LocalDate.parse(it, dateFmt) }.getOrNull() }
        ?: endDateBase.minusDays(60)

    override fun getRefreshKey(state: PagingState<Int, BidNotice>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BidNotice> {
        val weekBack = params.key ?: 0
        val startDayBack = weekBack * DAYS_PER_PAGE

        // 이번 페이지에서 담당할 날짜 목록 계산
        val targetDates = (startDayBack until startDayBack + DAYS_PER_PAGE)
            .map { endDateBase.minusDays(it.toLong()) }
            .filter { it >= startDateLimit }

        if (targetDates.isEmpty()) {
            return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        }

        return try {
            // DAYS_PER_PAGE일치 병렬 fetch
            val notices = coroutineScope {
                targetDates.map { date ->
                    async { fetchDayAll(date) }
                }.awaitAll()
            }
                .flatten()
                .map { it.toModel(this.params.category ?: BidCategory.CNSTWK) }
                .sortedByDescending { it.bidNtceDt }

            val nextWeekBack = weekBack + 1
            val nextStartDate = endDateBase.minusDays((nextWeekBack * DAYS_PER_PAGE).toLong())
            val nextKey = if (nextStartDate < startDateLimit) null else nextWeekBack

            LoadResult.Page(data = notices, prevKey = null, nextKey = nextKey)
        } catch (e: Exception) {
            Log.e(TAG, "페이지 로드 실패 (weekBack=$weekBack, category=${this.params.category})", e)
            LoadResult.Error(e)
        }
    }

    // 하루치 전체 데이터 fetch (999건 초과 시 나머지 페이지 병렬 수집)
    private suspend fun fetchDayAll(date: LocalDate) = coroutineScope {
        val bgnDt = date.format(dateFmt) + "0000"
        val endDt = date.format(dateFmt) + "2359"

        val firstResponse = fetchDay(bgnDt = bgnDt, endDt = endDt, pageNo = 1)
        val totalCount = firstResponse.response?.body?.totalCount ?: 0
        val totalPages = ceil(totalCount.toFloat() / NUM_OF_ROWS).toInt().coerceAtLeast(1)

        if (totalPages <= 1) {
            firstResponse.response?.body?.items?.item ?: emptyList()
        } else {
            val firstItems = firstResponse.response?.body?.items?.item ?: emptyList()
            val extraItems = (2..totalPages).map { pageNo ->
                async { fetchDay(bgnDt = bgnDt, endDt = endDt, pageNo = pageNo) }
            }.awaitAll().flatMap { it.response?.body?.items?.item ?: emptyList() }
            firstItems + extraItems
        }
    }

    private suspend fun fetchDay(bgnDt: String, endDt: String, pageNo: Int) =
        when (params.category) {
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

    companion object {
        private const val TAG = "BidNoticePagingSource"
        const val NUM_OF_ROWS = 999
        private const val DAYS_PER_PAGE = 7  // 1페이지당 병렬 fetch할 날짜 수
    }
}
