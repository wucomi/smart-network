package com.hik.smartnetwork

import android.app.Application
import android.content.Context
import com.hik.smartnetwork.network.DiskCacheHostNetwork
import com.hik.smartnetwork.network.NetWorkObserver
import com.hik.smartnetwork.network.NetworkFinder
import com.hik.smartnetwork.url.SmartNetworkURLStreamHandler
import com.hik.smartnetwork.utils.Logger
import java.net.URL

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

        try {
            URL.setURLStreamHandlerFactory { protocol ->
                when (protocol.lowercase()) {
                    "http", "https" -> SmartNetworkURLStreamHandler()
                    else -> null
                }
            }
        } catch (e: Error) {
            Logger.error("URLStreamHandlerFactory already set", e)
        }
    }
}