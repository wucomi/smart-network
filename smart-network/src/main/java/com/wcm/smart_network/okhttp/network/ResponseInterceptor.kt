package com.hik.smartnetwork.okhttp.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

internal class ResponseInterceptor(private val finder: INetworkFinder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            val response = chain.proceed(request)
            return response
        } catch (e: IOException) {
            finder.changeNetwork(request.url.let {
                "${it.host}:${it.port}"
            })
            throw e
        }
    }
}