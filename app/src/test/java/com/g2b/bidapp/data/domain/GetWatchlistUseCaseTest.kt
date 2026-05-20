package com.g2b.bidapp.data.domain

import app.cash.turbine.test
import com.g2b.bidapp.domain.model.BidCategory
import com.g2b.bidapp.domain.model.BidStatus
import com.g2b.bidapp.domain.model.WatchedBid
import com.g2b.bidapp.domain.repository.WatchlistRepository
import com.g2b.bidapp.domain.usecase.GetWatchlistUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetWatchlistUseCaseTest {

    private lateinit var repository: WatchlistRepository
    private lateinit var useCase: GetWatchlistUseCase

    private val sampleBid = WatchedBid(
        id = "uuid-1",
        bidNtceNo = "20240101001",
        bidNtceNm = "테스트 공고",
        ntceInsttNm = "테스트 기관",
        dmInsttNm = null,
        bidNtceDt = null,
        bidClseDt = null,
        opengDt = null,
        presmptPrce = null,
        bdgtAmt = null,
        bidCategory = BidCategory.CNSTWK,
        currentStatus = BidStatus.REGISTERED,
        bidNtceDtlUrl = null,
        watchedAt = System.currentTimeMillis(),
        syncedAt = null,
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetWatchlistUseCase(repository)
    }

    @Test
    fun `키워드 없이 호출 시 전체 관심공고 Flow가 반환된다`() = runTest {
        every { repository.getWatchlistFlow() } returns flowOf(listOf(sampleBid))

        useCase().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(sampleBid, list[0])
            awaitComplete()
        }
    }

    @Test
    fun `키워드로 호출 시 키워드 필터 Flow가 반환된다`() = runTest {
        every { repository.getWatchlistByKeywordFlow("테스트") } returns flowOf(listOf(sampleBid))

        useCase(keyword = "테스트").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            awaitComplete()
        }
    }

    @Test
    fun `빈 문자열은 전체 목록을 반환한다`() = runTest {
        every { repository.getWatchlistFlow() } returns flowOf(emptyList())

        useCase(keyword = "").test {
            val list = awaitItem()
            assertEquals(0, list.size)
            awaitComplete()
        }
    }
}