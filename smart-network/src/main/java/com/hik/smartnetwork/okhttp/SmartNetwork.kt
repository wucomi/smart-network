package com.hik.smartnetwork.okhttp

import com.hik.smartnetwork.SmartNetwork
import com.hik.smartnetwork.network.INetworkFinder
import com.hik.smartnetwork.utils.Logger
import okhttp3.OkHttpClient
import javax.net.SocketFactory

class SmartNetworkOkhttpBuilder(
    private val clientBuilder: OkHttpClient.Builder,
) {
    private var networkFinder: INetworkFinder? = null

    /**
     * 设置 NetworkFinder 参数
     * @param finder Network查询器
     * @return SmartNetworkBuilder
     */
    fun setNetworkFinder(finder: INetworkFinder) = apply {
        this.networkFinder = finder
    }

    /**
     * 应用配置并返回 OkHttpClient.Builder
     * @return OkHttpClient.Builder
     */
    fun build(): OkHttpClient {
        val client = clientBuilder.build()
        if (client.socketFactory != SocketFactory.getDefault()) {
            Logger.warn("SmartNetwork can only be used when the Okhttp SocketFactory is not set")
        }
        return client.newBuilder().apply {
            val urlHolder = HttpUrlHolder()
            addInterceptor(HttpUrlInterceptor(urlHolder))
            val finder = networkFinder ?: SmartNetwork.finder
            addNetworkInterceptor(ResponseInterceptor(finder))
            socketFactory(SmartNetworkSocketFactory(urlHolder, finder))
        }.build()
    }
}

/**
 * 开启 SmartNetwork 配置
 * @return SmartNetworkBuilder
 */
fun OkHttpClient.Builder.smartNetwork(): SmartNetworkOkhttpBuilder {
    return SmartNetworkOkhttpBuilder(this)
}