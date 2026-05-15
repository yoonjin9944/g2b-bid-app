package com.g2b.bidapp.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RetryInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0

        while (shouldRetry(response) && attempt < MAX_RETRIES) {
            response.close()
            val delayMs = BASE_DELAY_MS * (1L shl attempt)
            Thread.sleep(delayMs)
            response = chain.proceed(request)
            attempt++
        }
        return response
    }

    private fun shouldRetry(response: Response): Boolean =
        response.code == 429 || response.code in 500..599

    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 1_000L
    }
}