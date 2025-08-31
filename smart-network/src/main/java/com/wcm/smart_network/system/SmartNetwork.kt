package com.wcm.smart_network.system

import android.app.Application
import com.wcm.smart_network.system.network.DiskCacheHostNetwork
import com.wcm.smart_network.system.network.IDiskCacheHostNetwork
import com.wcm.smart_network.system.connection.ConnectionPoolConfig
import com.wcm.smart_network.system.connection.DefaultTimeoutConfig
import com.wcm.smart_network.system.connection.IConnectionPool
import com.wcm.smart_network.system.connection.ITimeoutConfig
import com.wcm.smart_network.system.connection.SmartConnectionFactory
import com.wcm.smart_network.system.connection.TargetConnectionPool
import com.wcm.smart_network.system.network.INetworkFinder
import com.wcm.smart_network.system.network.NetWorkObserver
import com.wcm.smart_network.system.network.NetworkFinder
import com.wcm.smart_network.system.network.NetworkType
import com.wcm.smart_network.system.proxy.GlobalProxy
import com.wcm.smart_network.system.proxy.LocalProxyServer
import com.wcm.smart_network.system.proxy.ProtocolHandler
import java.util.concurrent.TimeUnit

object SmartNetwork {
    private var option: SmartNetworkOption = SmartNetworkOption()
    private lateinit var finder: INetworkFinder
    private lateinit var connectionPool: IConnectionPool
    private lateinit var proxyServer: LocalProxyServer
    private val globalProxy = GlobalProxy()
    fun init(application: Application, config: SmartNetworkOption.() -> Unit) {
        option.apply(config).let {
            if (it.diskCacheHostNetwork == null) {
                it.diskCacheHostNetwork = DiskCacheHostNetwork()
                    .apply {
                        init(application)
                    }
            }
            val netWorkObserver = NetWorkObserver().apply { init(application) }
            finder = NetworkFinder(
                netWorkObserver,
                it.diskCacheHostNetwork,
                it.strategy,
                it.hostStrategy,
                it.fastConnect
            )
            globalProxy.startGlobalProxy(it.port.toString())
            val connectionPoolConfig = it.connectionPoolConfig ?: ConnectionPoolConfig(
                5, 2, TimeUnit.MINUTES
            )
            val timeoutConfig = it.timeoutConfig ?: DefaultTimeoutConfig()
            val connectionFactory = SmartConnectionFactory(finder, timeoutConfig, it.fastConnect)
            connectionPool = TargetConnectionPool(connectionPoolConfig, connectionFactory)
            val protocolHandler = ProtocolHandler(connectionPool)
            proxyServer = LocalProxyServer(it.port, protocolHandler)
            proxyServer.start()
        }
    }

    fun stop() {
        globalProxy.stopGlobalProxy()
        proxyServer.stop()
        connectionPool.shutdown()
    }
}

class SmartNetworkOption {
    var strategy: List<NetworkType>? = null
    var hostStrategy: Map<String, List<NetworkType>>? = null
    var port: Int = 8888
    var fastConnect: Boolean = true
    var diskCacheHostNetwork: IDiskCacheHostNetwork? = null
    var connectionPoolConfig: ConnectionPoolConfig? = null
    var timeoutConfig: ITimeoutConfig? = null
}

