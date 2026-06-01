package com.g2b.bidapp.ui.splash

import app.cash.turbine.test
import com.g2b.bidapp.data.version.ApkDownloader
import com.g2b.bidapp.data.version.DownloadState
import com.g2b.bidapp.data.version.VersionCheckRepository
import com.g2b.bidapp.domain.model.VersionCheckResult
import io.github.jan.supabase.auth.Auth
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var versionCheckRepository: VersionCheckRepository
    private lateinit var apkDownloader: ApkDownloader
    private lateinit var auth: Auth

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        versionCheckRepository = mockk()
        apkDownloader = mockk()
        auth = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SplashViewModel(
        versionCheckRepository = versionCheckRepository,
        apkDownloader = apkDownloader,
        auth = auth,
    )

    @Test
    fun `버전 최신이고 세션 있으면 NavigateToMain`() = runTest {
        coEvery { versionCheckRepository.checkVersion() } returns VersionCheckResult.UpToDate
        coEvery { auth.currentSessionOrNull() } returns mockk()

        val vm = createViewModel()
        vm.uiState.test {
            assertTrue(awaitItem() is SplashUiState.Loading)
            testDispatcher.scheduler.advanceUntilIdle()
            val final = expectMostRecentItem()
            assertTrue(final is SplashUiState.NavigateToMain)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `버전 최신이고 세션 없으면 NavigateToLogin`() = runTest {
        coEvery { versionCheckRepository.checkVersion() } returns VersionCheckResult.UpToDate
        coEvery { auth.currentSessionOrNull() } returns null

        val vm = createViewModel()
        vm.uiState.test {
            assertTrue(awaitItem() is SplashUiState.Loading)
            testDispatcher.scheduler.advanceUntilIdle()
            val final = expectMostRecentItem()
            assertTrue(final is SplashUiState.NavigateToLogin)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `강제 업데이트 필요 시 ForceUpdate`() = runTest {
        coEvery { versionCheckRepository.checkVersion() } returns
                VersionCheckResult.ForceUpdate("https://dl.example.com/app.apk", "보안 업데이트")

        val vm = createViewModel()
        vm.uiState.test {
            assertTrue(awaitItem() is SplashUiState.Loading)
            testDispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is SplashUiState.ForceUpdate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `권장 업데이트 있으면 RecommendUpdate`() = runTest {
        coEvery { versionCheckRepository.checkVersion() } returns
                VersionCheckResult.RecommendUpdate("https://dl.example.com/app.apk", "성능 개선")

        val vm = createViewModel()
        vm.uiState.test {
            assertTrue(awaitItem() is SplashUiState.Loading)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem() is SplashUiState.RecommendUpdate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `버전체크 실패는 non-blocking으로 NavigateToLogin`() = runTest {
        coEvery { versionCheckRepository.checkVersion() } returns
                VersionCheckResult.Error("Network error")
        coEvery { auth.currentSessionOrNull() } returns null

        val vm = createViewModel()
        vm.uiState.test {
            assertTrue(awaitItem() is SplashUiState.Loading)
            testDispatcher.scheduler.advanceUntilIdle()
            val final = expectMostRecentItem()
            assertTrue(final is SplashUiState.NavigateToLogin)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `다운로드 진행률 순서대로 방출`() = runTest {
        val downloadUrl = "https://dl.example.com/app.apk"
        coEvery { versionCheckRepository.checkVersion() } returns
                VersionCheckResult.ForceUpdate(downloadUrl, "")
        every { apkDownloader.downloadApk(downloadUrl) } returns flowOf(
            DownloadState.Progress(0.0f),
            DownloadState.Progress(0.5f),
            DownloadState.Progress(1.0f),
            DownloadState.Done(mockk(relaxed = true)),
        )
        every { apkDownloader.installApk(any()) } returns Unit

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // ForceUpdate 상태 확인
        assertTrue(vm.uiState.value is SplashUiState.ForceUpdate)

        // 수집된 상태 기록
        val collectedStates = mutableListOf<SplashUiState>()
        val job = launch {
            vm.uiState.collect { collectedStates += it }
        }

        // 다운로드 시작
        vm.onForceUpdateConfirmed(downloadUrl)
        testDispatcher.scheduler.advanceUntilIdle()

        job.cancel()

        val fractions = collectedStates
            .filterIsInstance<SplashUiState.Downloading>()
            .map { it.progress }

        assertTrue("진행률이 비어있음", fractions.isNotEmpty())
        assertTrue("진행률이 단조증가 아님", fractions == fractions.sorted())
    }
}