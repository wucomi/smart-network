package com.wcm.smart_network.okhttp

import android.app.Application
import android.content.Context
import android.util.Log
import com.wcm.smart_network.okhttp.network.DiskCacheHostNetwork
import com.wcm.smart_network.okhttp.network.IDiskCacheHostNetwork
import com.wcm.smart_network.okhttp.network.NetWorkObserver
import com.wcm.smart_network.okhttp.network.NetworkFinder
import com.wcm.smart_network.okhttp.network.NetworkType
import com.wcm.smart_network.okhttp.network.ResponseInterceptor
import com.wcm.smart_network.okhttp.socket.HttpUrlHolder
import com.wcm.smart_network.okhttp.socket.HttpUrlInterceptor
import com.wcm.smart_network.okhttp.socket.SmartNetworkSocketFactory
import okhttp3.OkHttpClient
import javax.net.SocketFactory
import kotlin.concurrent.Volatile

object SmartNetwork {
    @Volatile
    @JvmStatic
    lateinit var appCtx: Application

    fun init(context: Context) {
        appCtx = context.applicationContext as Application
    }
}

class SmartNetworkBuilder(
    private val clientBuilder: OkHttpClient.Builder,
) {
    private var strategy: List<NetworkType>? = null
    private var hostStrategy: Map<String, List<NetworkType>>? = null
    private var diskCacheHostNetwork: IDiskCacheHostNetwork? = null

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
     * 设置 diskCacheHostNetwork 参数
     * @param cache 缓存策略
     * @return SmartNetworkBuilder
     */
    fun setDiskCacheHostNetwork(cache: IDiskCacheHostNetwork) = apply {
        this.diskCacheHostNetwork = cache
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
            val hostNetworkCache = diskCacheHostNetwork ?: DiskCacheHostNetwork().apply {
                init(SmartNetwork.appCtx)
            }
            val finder = NetworkFinder(
                NetWorkObserver().apply { init(SmartNetwork.appCtx) },
                hostNetworkCache,
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
 * @return SmartNetworkBuilder
 */
fun OkHttpClient.Builder.smartNetwork(): SmartNetworkBuilder {
    return SmartNetworkBuilder(this)
}