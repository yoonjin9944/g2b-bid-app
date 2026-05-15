package com.g2b.bidapp.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Named

class AuthInterceptor @Inject constructor(
    @Named("serviceKey") private val serviceKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newUrl = chain.request().url.newBuilder()
            .addEncodedQueryParameter("serviceKey", serviceKey)
            .addEncodedQueryParameter("type", "json")
            .build()
        val newRequest = chain.request().newBuilder().url(newUrl).build()
        return chain.proceed(newRequest)
    }

}