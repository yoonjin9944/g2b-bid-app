package com.g2b.bidapp.ui.bid

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.model.SearchParams
import com.g2b.bidapp.domain.repository.AuthRepository
import com.g2b.bidapp.domain.repository.WatchlistRepository
import com.g2b.bidapp.domain.usecase.AddToWatchlistUseCase
import com.g2b.bidapp.domain.usecase.GetBidNoticeListUseCase
import com.g2b.bidapp.domain.usecase.RemoveFromWatchlistUseCase
import com.g2b.bidapp.ui.bid.list.BidListViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BidListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = mockk<GetBidNoticeListUseCase>()
    private val authRepository = mockk<AuthRepository>()
    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var addToWatchlistUseCase: AddToWatchlistUseCase
    private lateinit var removeFromWatchlistUseCase: RemoveFromWatchlistUseCase
    private lateinit var viewModel: BidListViewModel

    @Before
    fun setUp() {
        watchlistRepository = mockk(relaxed = true)
        addToWatchlistUseCase = mockk(relaxed = true)
        removeFromWatchlistUseCase = mockk(relaxed = true)
        Dispatchers.setMain(testDispatcher)
        every { useCase(any()) } returns flowOf(PagingData.Companion.empty())
        viewModel = BidListViewModel(
            useCase,
            authRepository,
            watchlistRepository,
            addToWatchlistUseCase,
            removeFromWatchlistUseCase,
            SavedStateHandle()
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `초기 탭은 CNSTWK`() {
        Assert.assertEquals(BidCategory.CNSTWK, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `탭 변경 시 selectedTab 이 업데이트된다`() {
        viewModel.onTabSelected(BidCategory.SERVC)
        Assert.assertEquals(BidCategory.SERVC, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `탭 변경 시 useCase 가 새 파라미터로 재호출된다`() = runTest {
        val job = launch {
            viewModel.pagingDataFlow.collect {}
        }

        viewModel.onTabSelected(BidCategory.THNG)
        advanceUntilIdle()

        verify { useCase(match { it.category == BidCategory.THNG }) }

        job.cancel()
    }

    @Test
    fun `applySearchParams 호출 시 searchParams 가 업데이트된다`() {
        val params = SearchParams(keyword = "정보시스템")
        viewModel.applySearchParams(params)

        Assert.assertEquals("정보시스템", viewModel.uiState.value.searchParams.keyword)
    }

    @Test
    fun `clearSearchParams 호출 시 빈 SearchParams 로 초기화된다`() {
        viewModel.applySearchParams(SearchParams(keyword = "테스트"))
        viewModel.clearSearchParams()

        Assert.assertEquals(SearchParams(), viewModel.uiState.value.searchParams)
    }

    @Test
    fun `isEmpty 가 true 이면 searchParams 가 비어있다`() {
        viewModel.clearSearchParams()
        Assert.assertEquals(true, viewModel.uiState.value.searchParams.isEmpty)
    }

    private fun makeBidNotice(id: String) = BidNotice(
        bidNtceNo = id,
        bidNtceOrd = "00",
        bidNtceNm = "공고$id",
        ntceInsttNm = null,
        dmInsttNm = null,
        bidNtceDt = null,
        bidClseDt = null,
        opengDt = null,
        presmptPrce = null,
        bdgtAmt = null,
        bidCategory = BidCategory.CNSTWK,
        bidNtceDtlUrl = null,
    )
}