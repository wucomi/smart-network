package com.wcm.smart_network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

object NetWorkObserver : INetWorkObserver {
    private const val TAG = "NetWorkObserver"
    private var connectivityManager: ConnectivityManager? = null
    private val networkObservers = arrayListOf<INetworkChangedObserver>()
    private val networkInfos = arrayListOf<NetworkInfo>()

    fun init(context: Context): Boolean {
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (!initWifiCallback()) {
            return false
        }
        return initCellularCallback()
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
                        Log.d(TAG, "onWifiAvailable: network=$network")
                        val networkInfo = NetworkInfo(
                            network,
                            if (isExtranetWifi(network)) {
                                NetworkType.ExtranetWifi
                            } else {
                                NetworkType.IntranetWifi
                            }
                        )
                        networkInfos.add(networkInfo)
                        for (observer in networkObservers) {
                            observer.onNetConnected(networkInfo, networkInfos)
                        }
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        Log.d(TAG, "onWifiLost: network=$network")
                        networkInfos.removeIf { it.network == network }
                        for (observer in networkObservers) {
                            observer.onNetDisconnected(network, networkInfos)
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
                        Log.d(TAG, "onCellularAvailable: network=$network")
                        val networkInfo = NetworkInfo(network, NetworkType.Cellular)
                        networkInfos.add(networkInfo)
                        for (observer in networkObservers) {
                            observer.onNetConnected(networkInfo, networkInfos)
                        }
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        Log.d(TAG, "onCellularLost: network=$network")
                        networkInfos.removeIf { it.network == network }
                        for (observer in networkObservers) {
                            observer.onNetDisconnected(network, networkInfos)
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
}
