package com.hik.smartnetwork.okhttp

import android.app.Application
import android.content.Context
import android.util.Log
import com.hik.smartnetwork.okhttp.network.DiskCacheHostNetwork
import com.hik.smartnetwork.okhttp.network.INetworkFinder
import com.hik.smartnetwork.okhttp.network.NetWorkObserver
import com.hik.smartnetwork.okhttp.network.NetworkFinder
import com.hik.smartnetwork.okhttp.network.ResponseInterceptor
import com.hik.smartnetwork.okhttp.socket.HttpUrlHolder
import com.hik.smartnetwork.okhttp.socket.HttpUrlInterceptor
import com.hik.smartnetwork.okhttp.socket.SmartNetworkSocketFactory
import okhttp3.OkHttpClient
import javax.net.SocketFactory

object SmartNetwork {
    @Volatile
    @JvmStatic
    lateinit var appCtx: Application
    val finder by lazy {
        NetworkFinder(
            NetWorkObserver,
            DiskCacheHostNetwork,
            null,
            null
        )
    }

    fun init(context: Context) {
        appCtx = context.applicationContext as Application
        NetWorkObserver.apply { init(appCtx) }
        DiskCacheHostNetwork.apply {
            init(appCtx)
        }
    }
}

class SmartNetworkBuilder(
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
            Log.e(TAG, "SmartNetwork can only be used when the Okhttp SocketFactory is not set")
        }
        return client.newBuilder().apply {
            val urlHolder = HttpUrlHolder()
            addInterceptor(HttpUrlInterceptor(urlHolder))
            val finder = networkFinder ?: SmartNetwork.finder
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