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
 * 날짜 역방향 슬라이딩 페이지 전략
 *
 * key(daysBack) = 0 → 기준 종료일, 1 → 하루 전, 2 → 이틀 전 …
 * 하루 단위로 조회해 bidNtceDt 내림차순 정렬 → 전체 목록이 최신순 보장
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

            val notices = allDtos
                .map { it.toModel(this.params.category ?: BidCategory.CNSTWK) }
                .sortedByDescending { it.bidNtceDt }

            val nextDaysBack = daysBack + 1
            val nextDate = endDateBase.minusDays(nextDaysBack.toLong())
            val nextKey = if (nextDate < startDateLimit) null else nextDaysBack

            LoadResult.Page(data = notices, prevKey = null, nextKey = nextKey)
        } catch (e: Exception) {
            Log.e(TAG, "페이지 로드 실패 (daysBack=$daysBack, date=$targetDate, category=${this.params.category})", e)
            LoadResult.Error(e)
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
    }
}
