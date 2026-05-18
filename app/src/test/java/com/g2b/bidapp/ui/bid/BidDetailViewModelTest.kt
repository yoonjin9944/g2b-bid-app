package com.g2b.bidapp.ui.bid

import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.ui.bid.detail.BidDetailUiState
import com.g2b.bidapp.ui.bid.detail.BidDetailViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BidDetailViewModelTest {

    private lateinit var viewModel: BidDetailViewModel

    private val sampleNotice = BidNotice(
        bidNtceNo = "20250001",
        bidNtceOrd = "00",
        bidNtceNm = "테스트 공고",
        ntceInsttNm = "조달청",
        dmInsttNm = "국방부",
        bidNtceDt = null,
        bidClseDt = null,
        opengDt = null,
        presmptPrce = 100_000_000L,
        bdgtAmt = null,
        bidCategory = BidCategory.CNSTWK,
        bidNtceDtlUrl = "https://www.g2b.go.kr/",
        isWatched = false,
    )

    @Before
    fun setUp() {
        viewModel = BidDetailViewModel()
    }

    @Test
    fun `초기 상태는 Idle이다`() {
        assertTrue(viewModel.uiState.value is BidDetailUiState.Idle)
    }

    @Test
    fun `setNotice 호출 시 Success 상태로 전이된다`() {
        viewModel.setNotice(sampleNotice)

        val state = viewModel.uiState.value
        assertTrue(state is BidDetailUiState.Success)
    }

    @Test
    fun `setNotice 후 notice 원본 isWatched 값이 그대로 유지된다`() {
        viewModel.setNotice(sampleNotice) // isWatched = false

        val state = viewModel.uiState.value as BidDetailUiState.Success
        assertFalse(state.isWatched)
    }

    @Test
    fun `toggleWatchlist 호출 시 isWatched 값이 반전된다`() {
        viewModel.setNotice(sampleNotice) // isWatched = false

        viewModel.toggleWatchlist()

        val state = viewModel.uiState.value as BidDetailUiState.Success
        assertTrue(state.isWatched)
    }

    @Test
    fun `toggleWatchlist 두 번 호출 시 원래 상태로 복귀한다`() {
        viewModel.setNotice(sampleNotice) // isWatched = false

        viewModel.toggleWatchlist()
        viewModel.toggleWatchlist()

        val state = viewModel.uiState.value as BidDetailUiState.Success
        assertFalse(state.isWatched)
    }

    @Test
    fun `Idle 상태에서 toggleWatchlist 호출 시 상태가 변경되지 않는다`() {
        viewModel.toggleWatchlist()

        assertTrue(viewModel.uiState.value is BidDetailUiState.Idle)
    }

    @Test
    fun `isWatched=true 인 공고 setNotice 후 상태가 올바르다`() {
        val watchedNotice = sampleNotice.copy(isWatched = true)
        viewModel.setNotice(watchedNotice)

        val state = viewModel.uiState.value as BidDetailUiState.Success
        assertTrue(state.isWatched)
    }

    @Test
    fun `isWatched=true 공고에서 toggleWatchlist 호출 시 false로 전환된다`() {
        viewModel.setNotice(sampleNotice.copy(isWatched = true))

        viewModel.toggleWatchlist()

        val state = viewModel.uiState.value as BidDetailUiState.Success
        assertFalse(state.isWatched)
    }
}
