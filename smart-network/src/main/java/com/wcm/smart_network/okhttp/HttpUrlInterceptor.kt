package com.wcm.smart_network.okhttp

import okhttp3.Interceptor
import okhttp3.Response

class HttpUrlInterceptor(private val urlHolder: HttpUrlHolder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val request = chain.request()
            urlHolder.setUrl(request.url)
            val response = chain.proceed(request)
            return response
        } catch (e: Throwable) {
            throw e
        } finally {
            urlHolder.clear()
        }
    }
}