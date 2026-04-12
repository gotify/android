package com.github.gotify.api

import okhttp3.Interceptor
import okhttp3.Response

internal class CloudflareAccessInterceptor(
    private val clientId: String,
    private val clientSecret: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("CF-Access-Client-Id", clientId)
            .addHeader("CF-Access-Client-Secret", clientSecret)
            .build()
        return chain.proceed(request)
    }
}
