package com.g2b.bidapp.data.domain

import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidNotice
import com.g2b.bidapp.domain.repository.WatchlistRepository
import com.g2b.bidapp.domain.usecase.AddToWatchlistUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddToWatchlistUseCaseTest {

    private lateinit var repository: WatchlistRepository
    private lateinit var useCase: AddToWatchlistUseCase

    private val sampleNotice = BidNotice(
        bidNtceNo = "20240101001",
        bidNtceOrd = "00",
        bidNtceNm = "테스트 공고",
        ntceInsttNm = "테스트 기관",
        dmInsttNm = null,
        bidNtceDt = null,
        bidClseDt = null,
        opengDt = null,
        presmptPrce = null,
        bdgtAmt = null,
        bidCategory = BidCategory.CNSTWK,
        bidNtceDtlUrl = null,
    )

    @Before
    fun setUp() {
        repository = mockk()
    }

    @Test
    fun `미등록 공고는 성공적으로 등록된다`() = runTest {
        coEvery { repository.isWatched(sampleNotice.bidNtceNo) } returns false
        coEvery { repository.addToWatchlist(sampleNotice) } returns Result.success(Unit)

        useCase = AddToWatchlistUseCase(repository)
        val result = useCase(sampleNotice)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.addToWatchlist(sampleNotice) }
    }

    @Test
    fun `이미 등록된 공고는 중복 등록하지 않는다`() = runTest {
        coEvery { repository.isWatched(sampleNotice.bidNtceNo) } returns true

        useCase = AddToWatchlistUseCase(repository)
        val result = useCase(sampleNotice)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { repository.addToWatchlist(any()) }
    }

    @Test
    fun `repository가 실패를 반환하면 실패 결과가 전달된다`() = runTest {
        val exception = RuntimeException("네트워크 오류")
        coEvery { repository.isWatched(sampleNotice.bidNtceNo) } returns false
        coEvery { repository.addToWatchlist(sampleNotice) } returns Result.failure(exception)

        useCase = AddToWatchlistUseCase(repository)
        val result = useCase(sampleNotice)

        assertTrue(result.isFailure)
        coVerify(exactly = 1) { repository.addToWatchlist(sampleNotice) }
    }
}