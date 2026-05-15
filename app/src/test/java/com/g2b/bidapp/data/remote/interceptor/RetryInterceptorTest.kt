package com.g2b.bidapp.data.remote.interceptor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
            .build()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `첫 번째 요청 성공 시 재시도 없이 반환`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val response = client.newCall(Request.Builder().url(server.url("/")).build()).execute()

        assertEquals(200, response.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `429 응답 후 재시도하여 200 성공`() {
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val response = client.newCall(Request.Builder().url(server.url("/")).build()).execute()

        assertEquals(200, response.code)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `5xx 3회 연속 후 마지막 응답 반환`() {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(500)) }

        val response = client.newCall(Request.Builder().url(server.url("/")).build()).execute()

        assertEquals(500, response.code)
        assertEquals(4, server.requestCount) // 최초 1 + 재시도 3
    }

    @Test
    fun `4xx 클라이언트 오류는 재시도 없이 반환`() {
        server.enqueue(MockResponse().setResponseCode(400))

        val response = client.newCall(Request.Builder().url(server.url("/")).build()).execute()

        assertEquals(400, response.code)
        assertEquals(1, server.requestCount)
    }
}