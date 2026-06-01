package com.g2b.bidapp.ui.watchlist

import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidStatus
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.domain.repository.WatchlistRepository
import com.g2b.bidapp.domain.usecase.GetWatchlistUseCase
import com.g2b.bidapp.domain.usecase.RemoveFromWatchlistUseCase
import com.g2b.bidapp.ui.bid.watchlist.WatchlistViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var getWatchlistUseCase: GetWatchlistUseCase
    private lateinit var removeFromWatchlistUseCase: RemoveFromWatchlistUseCase
    private lateinit var viewModel: WatchlistViewModel

    private val sampleBid = WatchedBid(
        id = "uuid-1",
        bidNtceNo = "20240101001",
        bidNtceNm = "테스트 공고",
        ntceInsttNm = "테스트 기관",
        dmInsttNm = null,
        bidNtceDt = null,
        bidClseDt = System.currentTimeMillis() + 86400_000L,
        opengDt = null,
        presmptPrce = 100_000_000L,
        bdgtAmt = null,
        bidCategory = BidCategory.CNSTWK,
        currentStatus = BidStatus.REGISTERED,
        bidNtceDtlUrl = null,
        watchedAt = System.currentTimeMillis(),
        syncedAt = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        watchlistRepository = mockk()
        getWatchlistUseCase = GetWatchlistUseCase(watchlistRepository)
        removeFromWatchlistUseCase = RemoveFromWatchlistUseCase(watchlistRepository)

        every { watchlistRepository.getWatchlistFlow() } returns flowOf(listOf(sampleBid))
        every { watchlistRepository.getWatchlistByKeywordFlow(any()) } returns flowOf(emptyList())

        viewModel = WatchlistViewModel(
            getWatchlistUseCase = getWatchlistUseCase,
            removeFromWatchlistUseCase = removeFromWatchlistUseCase,
            watchlistRepository = watchlistRepository,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // coroutine 실행 전이므로 combine이 아직 시작되지 않아 initialValue(isLoading=true)를 반환
    @Test
    fun `초기 상태에서 isLoading이 true이다`() = runTest {
        assertEquals(true, viewModel.uiState.value.isLoading)
    }

    // WhileSubscribed: 구독자가 생겨야 combine이 시작됨 → backgroundScope로 구독 활성화 후 검증
    @Test
    fun `Room Flow 첫 방출 후 isLoading이 false가 된다`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `키워드 변경 시 keyword 상태가 갱신된다`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onKeywordChange("바우처")
        advanceUntilIdle()

        assertEquals("바우처", viewModel.uiState.value.keyword)
    }

    @Test
    fun `deleteItem 호출 시 pendingDeleteBid가 설정된다`() = runTest {
        coEvery { watchlistRepository.removeFromWatchlist(any()) } returns Result.success(Unit)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.deleteItem(sampleBid)
        advanceUntilIdle()

        assertEquals(sampleBid, viewModel.uiState.value.pendingDeleteBid)
        coVerify(exactly = 1) { watchlistRepository.removeFromWatchlist(sampleBid.bidNtceNo) }
    }

    @Test
    fun `confirmDelete 호출 시 pendingDeleteBid가 null이 된다`() = runTest {
        coEvery { watchlistRepository.removeFromWatchlist(any()) } returns Result.success(Unit)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.deleteItem(sampleBid)
        advanceUntilIdle()

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingDeleteBid)
    }

    @Test
    fun `undoDelete 호출 시 restoreWatchedBid가 호출된다`() = runTest {
        coEvery { watchlistRepository.removeFromWatchlist(any()) } returns Result.success(Unit)
        coEvery { watchlistRepository.restoreWatchedBid(any()) } returns Result.success(Unit)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.deleteItem(sampleBid)
        advanceUntilIdle()

        viewModel.undoDelete(sampleBid)
        advanceUntilIdle()

        coVerify(exactly = 1) { watchlistRepository.restoreWatchedBid(sampleBid) }
        assertNull(viewModel.uiState.value.pendingDeleteBid)
    }

    @Test
    fun `undoDelete 호출 후 pendingDeleteBid가 null이 된다`() = runTest {
        coEvery { watchlistRepository.removeFromWatchlist(any()) } returns Result.success(Unit)
        coEvery { watchlistRepository.restoreWatchedBid(any()) } returns Result.success(Unit)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.deleteItem(sampleBid)
        advanceUntilIdle()

        viewModel.undoDelete(sampleBid)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingDeleteBid)
    }
}
