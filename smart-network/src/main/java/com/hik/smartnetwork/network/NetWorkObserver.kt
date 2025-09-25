package com.hik.smartnetwork.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.hik.smartnetwork.utils.Logger
import com.hik.smartnetwork.utils.removeIfa
import java.net.NetworkInterface
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

object NetWorkObserver : INetWorkObserver {
    private var connectivityManager: ConnectivityManager? = null
    private val networkObservers = CopyOnWriteArrayList<INetworkChangedObserver>()
    private val networkInfos = CopyOnWriteArrayList<NetworkInfo>()

    fun init(context: Context) {
        if (connectivityManager == null) {
            connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (initWifiCallback()) {
                initCellularCallback()
            }
        }
    }

    override fun registerNetworkObserver(observer: INetworkChangedObserver) {
        if (!networkObservers.contains(observer)) {
            networkObservers.add(observer)
            if (networkInfos.isNotEmpty()) {
                observer.onNetConnected(networkInfos.last(), networkInfos)
            }
        }
    }

    override fun unregisterNetworkObserver(observer: INetworkChangedObserver) {
        networkObservers.remove(observer)
    }

    private fun initWifiCallback(): Boolean {
        val builderWifi: NetworkRequest.Builder = NetworkRequest.Builder()
        builderWifi.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        val requestWifi = builderWifi.build()
        try {
            connectivityManager?.requestNetwork(
                requestWifi,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        Logger.debug("onWifiAvailable: network=$network")
                        networkInfos.removeIfa { it.network.networkHandle == network.networkHandle }
                        networkInfos.filter { it.networkType != NetworkType.Cellular }
                            .forEach {
                                Logger.debug("Wifi lost on new network available: network=${it.network}")
                                networkInfos.remove(it)
                                for (observer in networkObservers) {
                                    observer.onNetDisconnected(it.network, networkInfos)
                                }
                            }
                        val networkInfo = NetworkInfo(
                            network,
                            if (isExtranetWifi(network)) {
                                NetworkType.ExtranetWifi
                            } else {
                                NetworkType.IntranetWifi
                            },
                            isVpn(network),
                            connectivityManager?.getNetworkCapabilities(network)
                        )
                        networkInfos.add(networkInfo)
                        for (observer in networkObservers) {
                            observer.onNetConnected(networkInfo, networkInfos)
                        }
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        val isContains = networkInfos.removeIfa {
                            it.network.networkHandle == network.networkHandle
                        }
                        if (!isContains) {
                            return
                        }
                        Logger.debug("onWifiLost: network=$network")
                        for (observer in networkObservers) {
                            observer.onNetDisconnected(network, networkInfos)
                        }
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        super.onCapabilitiesChanged(network, networkCapabilities)
                        Logger.debug("onWifiCapabilitiesChanged: network=$network, networkCapabilities=$networkCapabilities")
                        networkInfos.removeIfa { it.network.networkHandle == network.networkHandle }
                        networkInfos.add(
                            NetworkInfo(
                                network,
                                if (isExtranetWifi(network)) {
                                    NetworkType.ExtranetWifi
                                } else {
                                    NetworkType.IntranetWifi
                                },
                                isVpn(network),
                                networkCapabilities
                            )
                        )
                        for (observer in networkObservers) {
                            observer.onCapabilitiesChanged(network, networkInfos)
                        }
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun initCellularCallback(): Boolean {
        val builderNetwork: NetworkRequest.Builder = NetworkRequest.Builder()
        builderNetwork.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        builderNetwork.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        val requestNetwork = builderNetwork.build()

        try {
            connectivityManager?.requestNetwork(
                requestNetwork,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        val isContains = networkInfos.indexOfFirst {
                            it.network.networkHandle == network.networkHandle
                        } != -1
                        if (isContains) {
                            return
                        }
                        Logger.debug("onCellularAvailable: network=$network")
                        val networkInfo = NetworkInfo(
                            network, NetworkType.Cellular,
                            isVpn(network),
                            connectivityManager?.getNetworkCapabilities(network)
                        )
                        networkInfos.add(networkInfo)
                        for (observer in networkObservers) {
                            observer.onNetConnected(networkInfo, networkInfos)
                        }
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        val isContains = networkInfos.removeIfa {
                            it.network.networkHandle == network.networkHandle
                        }
                        if (!isContains) {
                            return
                        }
                        Logger.debug("onCellularLost: network=$network")
                        for (observer in networkObservers) {
                            observer.onNetDisconnected(network, networkInfos)
                        }
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        super.onCapabilitiesChanged(network, networkCapabilities)
                        val index = networkInfos.indexOfFirst {
                            it.network.networkHandle == network.networkHandle
                        }
                        if (index == -1) {
                            return
                        }
                        Logger.debug("onCellularChanged: network=$network, networkCapabilities=$networkCapabilities")
                        networkInfos[index] = NetworkInfo(
                            network,
                            NetworkType.Cellular,
                            isVpn(network),
                            networkCapabilities
                        )
                        for (observer in networkObservers) {
                            observer.onCapabilitiesChanged(network, networkInfos)
                        }

                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun isExtranetWifi(network: Network): Boolean {
        val networkCapabilities = connectivityManager?.getNetworkCapabilities(network)
        return networkCapabilities?.let {
            // 检查是否为 WiFi 网络
            it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    // 检查是否具备互联网访问能力
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    // 检查网络是否已建立连接
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                    // 检查网络是否不受限制
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
        } ?: false
    }

    fun isVpn(network: Network): Boolean {
        return isVpnActive(network) || isVpnEnabledByInterface() || checkVpnByRouting()
    }

    fun isVpnActive(network: Network): Boolean {
        val capabilities = connectivityManager?.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                        || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN))
    }

    // 检查网络接口名称
    fun isVpnEnabledByInterface(): Boolean {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            return networkInterfaces.any { it.name.contains("tun") || it.name.contains("ppp") }
        } catch (e: Exception) {
            Logger.error("isVpnEnabledByInterface error", e)
        }
        return false
    }

    // 检查路由表
    fun checkVpnByRouting(): Boolean {
        try {
            val process = Runtime.getRuntime().exec("ip route show table all")
            val output = process.inputStream.bufferedReader().use { it.readText() }
            return output.contains("tun") || output.contains("ppp")
        } catch (e: Exception) {
            Logger.error("checkVpnByRouting error", e)
        }
        return false
    }
}
