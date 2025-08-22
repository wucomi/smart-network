package com.wcm.smart_network

import android.util.Log
import okhttp3.OkHttpClient
import javax.net.SocketFactory

class SmartNetworkBuilder(
    private val clientBuilder: OkHttpClient.Builder,
    private val networkObserver: INetWorkObserver
) {
    private var diskCacheHostNetwork: IDiskCacheHostNetwork? = null
    private var strategy: List<NetworkType>? = null
    private var hostStrategy: Map<String, List<NetworkType>>? = null

    /**
     * 设置 diskCacheHostNetwork 参数
     * @param cache 缓存策略
     * @return SmartNetworkBuilder
     */
    fun setDiskCacheHostNetwork(cache: IDiskCacheHostNetwork) = apply {
        this.diskCacheHostNetwork = cache
    }

    /**
     * 设置 strategy 参数
     * @param strategyList 全局网络策略
     * @return SmartNetworkBuilder
     */
    fun setStrategy(strategyList: List<NetworkType>) = apply {
        this.strategy = strategyList
    }

    /**
     * 设置 hostStrategy 参数
     * @param strategyMap host对应的网络策略
     * @return SmartNetworkBuilder
     */
    fun setHostStrategy(strategyMap: Map<String, List<NetworkType>>) = apply {
        this.hostStrategy = strategyMap
    }

    /**
     * 应用配置并返回 OkHttpClient.Builder
     * @return OkHttpClient.Builder
     */
    fun build(): OkHttpClient {
        val client = clientBuilder.build()
        if (client.socketFactory != SocketFactory.getDefault()) {
            Log.e(TAG, "SmartNetwork can only be used when the Okhttp SocketFactory is not set")
        }
        return client.newBuilder().apply {
            val urlHolder = HttpUrlHolder()
            addInterceptor(HttpUrlInterceptor(urlHolder))
            val finder = NetworkFinder(
                networkObserver,
                client.dispatcher,
                diskCacheHostNetwork,
                strategy,
                hostStrategy
            )
            addNetworkInterceptor(ResponseInterceptor(finder))
            socketFactory(SmartNetworkSocketFactory(urlHolder, finder))
        }.build()
    }

    companion object {
        const val TAG = "SmartNetwork"
    }
}

/**
 * 开启 SmartNetwork 配置
 * @param networkObserver 网络状态观察者
 * @return SmartNetworkBuilder
 */
fun OkHttpClient.Builder.smartNetwork(
    networkObserver: INetWorkObserver
): SmartNetworkBuilder {
    return SmartNetworkBuilder(this, networkObserver)
}