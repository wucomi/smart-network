package com.wcm.smart_network.system.network

import android.net.Network
import com.wcm.smart_network.okhttp.utils.Logger
import java.util.concurrent.ConcurrentHashMap

class NetworkFinder(
    networkObserver: INetWorkObserver,
    private val diskCacheHostNetwork: IDiskCacheHostNetwork? = null,
    private val strategy: List<NetworkType>?,
    private val hostStrategy: Map<String, List<NetworkType>>? = null,
    private val fastConnect: Boolean,
) : INetworkFinder {
    private val addressNetworkInfos = ConcurrentHashMap<String, NetworkInfo>()
    private var networkInfos: List<NetworkInfo> = arrayListOf()

    init {
        networkObserver.registerNetworkObserver(object : INetworkChangedObserver {
            override fun onNetConnected(networkInfo: NetworkInfo, networkInfos: List<NetworkInfo>) {
                this@NetworkFinder.networkInfos = networkInfos
            }

            override fun onNetDisconnected(network: Network, networkInfos: List<NetworkInfo>) {
                this@NetworkFinder.networkInfos = networkInfos
                val removeKeys = addressNetworkInfos.filter {
                    it.value.network == network
                }.keys
                removeKeys.forEach { removeNetwork(it) }
            }
        })
    }

    override fun findNetwork(address: String): NetworkInfo? {
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

        return null
    }

    override fun putNetwork(address: String, networkInfo: NetworkInfo) {
        addressNetworkInfos[address] = networkInfo
        diskCacheHostNetwork?.putAddressNetwork(address, networkInfo.network.networkHandle)
        Logger.debug("Put Network: address=$address, network=$networkInfo")
    }

    override fun getSortNetworks(address: String): List<NetworkInfo> {
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

    override fun changeNetwork(address: String) {
        val index = (networkInfos.indexOfFirst {
            it.network == addressNetworkInfos[address]?.network
        } + 1).coerceIn(networkInfos.indices)
        val networkInfo = networkInfos[index]
        addressNetworkInfos[address] = networkInfo
        diskCacheHostNetwork?.putAddressNetwork(address, networkInfo.network.networkHandle)
    }

    override fun removeNetwork(address: String) {
        addressNetworkInfos.remove(address)
        diskCacheHostNetwork?.removeAddressNetwork(address)
    }

    override fun clear() {
        addressNetworkInfos.clear()
        diskCacheHostNetwork?.clear()
    }
}