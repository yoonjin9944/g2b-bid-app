package com.g2b.bidapp.data.version

import com.g2b.bidapp.domain.model.VersionCheckResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VersionCheckRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: TestableVersionCheckRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        repository = TestableVersionCheckRepository(
            okHttpClient = OkHttpClient(),
            testUrl = mockWebServer.url("/version.json").toString(),
            testVersionName = "1.0.0",
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `현재 버전이 최신이면 UP_TO_DATE 반환`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody(
                """{"latestVersion":"1.0.0","minRequiredVersion":"1.0.0",
                   "downloadUrl":"https://example.com/app.apk","releaseNotes":""}"""
            )
        )
        val result = repository.checkVersion()
        println("=== 실제 결과: $result ===")
        assertTrue(result is VersionCheckResult.UpToDate)
    }

    @Test
    fun `최신 버전이 현재보다 높으면 RECOMMEND_UPDATE 반환`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody(
                """{"latestVersion":"1.1.0","minRequiredVersion":"1.0.0",
                   "downloadUrl":"https://example.com/app.apk","releaseNotes":"성능 개선"}"""
            )
        )
        val result = repository.checkVersion()
        assertTrue(result is VersionCheckResult.RecommendUpdate)
        assertEquals("https://example.com/app.apk", (result as VersionCheckResult.RecommendUpdate).downloadUrl)
    }

    @Test
    fun `minRequiredVersion이 현재보다 높으면 FORCE_UPDATE 반환`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody(
                """{"latestVersion":"2.0.0","minRequiredVersion":"1.1.0",
                   "downloadUrl":"https://example.com/app.apk","releaseNotes":"보안 업데이트"}"""
            )
        )
        val result = repository.checkVersion()
        assertTrue(result is VersionCheckResult.ForceUpdate)
    }

    @Test
    fun `네트워크 오류 시 ERROR 반환 (non-blocking)`() = runTest {
        mockWebServer.shutdown()   // 연결 거부 유도
        val result = repository.checkVersion()
        assertTrue(result is VersionCheckResult.Error)
    }

    @Test
    fun `잘못된 JSON 응답 시 ERROR 반환 (non-blocking)`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody("NOT_A_JSON_RESPONSE")
        )
        val result = repository.checkVersion()
        assertTrue(result is VersionCheckResult.Error)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TestableVersionCheckRepository(
    okHttpClient: OkHttpClient,
    private val testUrl: String,
    private val testVersionName: String,
) : VersionCheckRepository(
    okHttpClient = okHttpClient
) {
    override val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    override val versionJsonUrl: String get() = testUrl
    override val currentVersion: String get() = testVersionName
}