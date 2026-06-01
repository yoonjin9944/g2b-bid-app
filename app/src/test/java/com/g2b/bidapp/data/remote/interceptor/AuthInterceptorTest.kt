package com.g2b.bidapp.data.remote.interceptor

import junit.framework.TestCase.assertTrue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(serviceKey = "TEST_KEY"))
            .build()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `serviceKey 가 자동으로 쿼리 파라미터에 추가된다`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recordedUrl = server.takeRequest().requestUrl.toString()
        assertTrue("serviceKey 포함 확인", recordedUrl.contains("serviceKey=TEST_KEY"))
    }

    @Test
    fun `type=json 이 자동으로 추가된다`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recordedUrl = server.takeRequest().requestUrl.toString()
        Assert.assertTrue("type=json 포함 확인", recordedUrl.contains("type=json"))
    }

    @Test
    fun `기존 쿼리 파라미터는 유지된다`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.newCall(
            Request.Builder().url(server.url("/test?pageNo=1")).build()
        ).execute()

        val recordedUrl = server.takeRequest().requestUrl.toString()
        Assert.assertTrue("기존 파라미터 유지", recordedUrl.contains("pageNo=1"))
        Assert.assertTrue("serviceKey 추가", recordedUrl.contains("serviceKey=TEST_KEY"))
    }
}