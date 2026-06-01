package com.g2b.bidapp.data.remote.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import com.g2b.bidapp.data.remote.api.BidPublicInfoApi
import com.g2b.bidapp.data.remote.dto.BidApiBody
import com.g2b.bidapp.data.remote.dto.BidApiResponse
import com.g2b.bidapp.data.remote.dto.BidNoticeDto
import com.g2b.bidapp.data.remote.dto.BidNoticeItems
import com.g2b.bidapp.data.remote.dto.BidNoticeListResponse
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.SearchParams
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BidNoticePagingSourceTest {

    private val api = mockk<BidPublicInfoApi>()
    private val params = SearchParams(category = BidCategory.CNSTWK)

    private fun makeEmptyItems() = BidNoticeItems(item = emptyList())

    private fun makeDto(id: String) = BidNoticeDto(
        bidNtceNo = id, bidNtceOrd = "00", bidNtceNm = "공고$id",
        ntceInsttNm = null, dmInsttNm = null, bidNtceDt = null,
        bidClseDt = null, opengDt = null, presmptPrce = null,
        bdgtAmt = null, ntceKindNm = null, bidNtceDtlUrl = null,
    )

    private fun makeResponse(items: BidNoticeItems, total: Int, page: Int = 1) =
        BidNoticeListResponse(
            response = BidApiResponse(
                header = null,
                body = BidApiBody(items = items, numOfRows = 20, pageNo = page, totalCount = total),
            )
        )

    @Test
    fun `첫 페이지 로드 시 nextKey 가 올바르게 계산된다`() = runTest {
        val items = BidNoticeItems(item = (1..20).map { makeDto("$it") })
        coEvery { api.getCnstwkList(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                makeResponse(items, total = 45)

        val pager = TestPager(
            config = PagingConfig(pageSize = 20),
            pagingSource = BidNoticePagingSource(api, params),
        )

        val result = pager.refresh() as PagingSource.LoadResult.Page
        assertEquals(20, result.data.size)
        assertEquals(2, result.nextKey)
    }

    @Test
    fun `마지막 페이지에서 nextKey 가 null 반환`() = runTest {
        val items = BidNoticeItems(item = (1..5).map { makeDto("$it") })
        coEvery { api.getCnstwkList(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                makeResponse(items, total = 5)

        val pager = TestPager(
            config = PagingConfig(pageSize = 20),
            pagingSource = BidNoticePagingSource(api, params),
        )

        val result = pager.refresh() as PagingSource.LoadResult.Page
        assertEquals(5, result.data.size)
        assertNull(result.nextKey)
    }

    @Test
    fun `totalCount 0 이면 빈 목록과 nextKey null 반환`() = runTest {
        coEvery { api.getCnstwkList(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                makeResponse(makeEmptyItems(), total = 0)

        val pager = TestPager(
            config = PagingConfig(pageSize = 20),
            pagingSource = BidNoticePagingSource(api, params),
        )

        val result = pager.refresh() as PagingSource.LoadResult.Page
        assertTrue(result.data.isEmpty())
        assertNull(result.nextKey)
    }

    @Test
    fun `API 예외 시 LoadResult Error 반환`() = runTest {
        coEvery { api.getCnstwkList(any(), any(), any(), any(), any(), any(), any(), any()) } throws
                RuntimeException("네트워크 오류")

        val pager = TestPager(
            config = PagingConfig(pageSize = 20),
            pagingSource = BidNoticePagingSource(api, params),
        )

        val result = pager.refresh()
        assertTrue(result is PagingSource.LoadResult.Error)
    }

    @Test
    fun `SERVC 카테고리 선택 시 getServcList 가 호출된다`() = runTest {
        val servcParams = SearchParams(category = BidCategory.SERVC)
        coEvery { api.getServcList(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                makeResponse(makeEmptyItems(), total = 0)

        val pager = TestPager(
            config = PagingConfig(pageSize = 20),
            pagingSource = BidNoticePagingSource(api, servcParams),
        )

        val result = pager.refresh()
        assertTrue(result is PagingSource.LoadResult.Page)
    }
}