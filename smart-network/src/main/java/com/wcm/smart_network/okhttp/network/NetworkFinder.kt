package com.wcm.smart_network.okhttp.network

import android.net.Network
import android.os.Looper
import com.wcm.smart_network.okhttp.utils.Logger
import okhttp3.internal.closeQuietly
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

internal class NetworkFinder(
    networkObserver: INetWorkObserver,
    private val diskCacheHostNetwork: IDiskCacheHostNetwork? = null,
    private val strategy: List<NetworkType>?,
    private val hostStrategy: Map<String, List<NetworkType>>? = null,
) {
    private val addressNetworkInfos = ConcurrentHashMap<String, NetworkInfo>()
    private var networkInfos: List<NetworkInfo> = arrayListOf()
    private val dispatcher = Executors.newCachedThreadPool()

    init {
        networkObserver.registerNetworkObserver(object : INetworkChangedObserver {
            override fun onNetConnected(networkInfo: NetworkInfo, networkInfos: List<NetworkInfo>) {
                this@NetworkFinder.networkInfos = networkInfos
                if (networkInfo.isVpn) {
                    val removeKeys = addressNetworkInfos.filter {
                        it.value.network == networkInfo.network
                    }.keys
                    removeKeys.forEach { removeNetwork(it) }
                }
            }

            override fun onNetDisconnected(network: Network, networkInfos: List<NetworkInfo>) {
                this@NetworkFinder.networkInfos = networkInfos
                val removeKeys = addressNetworkInfos.filter {
                    it.value.network == network
                }.keys
                removeKeys.forEach { removeNetwork(it) }
            }

            override fun onCapabilitiesChanged(network: Network, networkInfos: List<NetworkInfo>) {
                this@NetworkFinder.networkInfos = networkInfos
                val removeKeys = addressNetworkInfos.filter {
                    it.value.network == network
                }.keys
                removeKeys.forEach { removeNetwork(it) }
            }
        })
    }

    fun find(address: String?): NetworkInfo? {
        address ?: return null
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalStateException("Cannot find network on the main thread.")
        }
        var host: String? = null
        var port = 80
        address.split(":").takeIf { list -> list.size == 2 }
            ?.run {
                host = get(0)
                port = get(1).toIntOrNull() ?: 80
            }
        Logger.debug("Find Network: address=$address, host=$host, port=$port")
        // 从内存中获取
        addressNetworkInfos[address]?.let {
            Logger.debug("Find Network from memory: address=$address, network=$it")
            return it
        }
        // 从DiskCache中获取，和当前网络环境匹配的保存到内存返回，不匹配的跳过
        diskCacheHostNetwork?.getAddressNetwork(address)?.let { networkHandle ->
            val networkInfo = networkInfos.find {
                networkHandle == it.network.networkHandle
            }
            networkInfo?.let {
                addressNetworkInfos[address] = it
                Logger.debug("Find Network from disk cache: address=$address, network=$it")
                return it
            }
        }

        // 通过检测网络连通性获取
        val sortNetworkInfos = sortNetworkInfos(address)
        // 快速匹配
        var networkInfo: NetworkInfo? = null
        val countDownLatch = CountDownLatch(2)
        val connections = sortNetworkInfos.map { NConnection(it, Socket()) }
            .onEach { connection ->
                dispatcher.execute {
                    if (isReachable(connection, host, port)) {
                        networkInfo = connection.networkInfo
                        (0 until countDownLatch.count).forEach { _ ->
                            countDownLatch.countDown()
                        }
                    } else {
                        countDownLatch.countDown()
                    }
                }
            }
        countDownLatch.await()
        // 将其他连接断开, 避免占用线程
        connections.forEach {
            it.socket.closeQuietly()
        }
        return networkInfo?.apply {
            addressNetworkInfos[address] = this
            diskCacheHostNetwork?.putAddressNetwork(address, network.networkHandle)
            Logger.debug("Find Network from check reachable: network=${this}")
        }
    }

    private fun isReachable(connection: NConnection, host: String?, port: Int) = runCatching {
        val socket = connection.socket
        val networkInfo = connection.networkInfo
        networkInfo.network.bindSocket(socket)
        socket.use {
            if (host.isNullOrBlank() || host == "null") {
                it.connect(
                    InetSocketAddress(port),
                    2000,
                )
            } else {
                it.connect(
                    InetSocketAddress(host, port),
                    2000,
                )
            }
        }
    }.isSuccess

    private fun sortNetworkInfos(address: String?): ArrayList<NetworkInfo> {
        val strategy = hostStrategy?.get(address) ?: run {
            strategy
        } ?: run {
            arrayListOf(
                NetworkType.ExtranetWifi,
                NetworkType.Cellular,
                NetworkType.IntranetWifi,
            )
        }
        val sortNetworkInfos = arrayListOf<NetworkInfo>()
        strategy.forEach { networkType ->
            networkInfos.firstOrNull {
                it.networkType == networkType
            }?.let {
                sortNetworkInfos.add(it)
            }
        }
        Logger.debug("Sort Network: sortNetworks=$sortNetworkInfos, networks=$networkInfos")
        return sortNetworkInfos
    }

    fun changeNetwork(address: String) {
        val index = (networkInfos.indexOfFirst {
            it.network == addressNetworkInfos[address]?.network
        } + 1).coerceIn(networkInfos.indices)
        val networkInfo = networkInfos[index]
        addressNetworkInfos[address] = networkInfo
        diskCacheHostNetwork?.putAddressNetwork(address, networkInfo.network.networkHandle)
    }

    fun removeNetwork(address: String) {
        addressNetworkInfos.remove(address)
        diskCacheHostNetwork?.removeAddressNetwork(address)
    }

    companion object {
        private const val TAG = "SmartNetwork"
    }
}