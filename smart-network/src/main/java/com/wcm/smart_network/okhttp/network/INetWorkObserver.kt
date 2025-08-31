package com.wcm.smart_network.okhttp.network

import android.net.Network

interface INetWorkObserver {
    fun registerNetworkObserver(observer: INetworkChangedObserver)
    fun unregisterNetworkObserver(observer: INetworkChangedObserver)
}

interface INetworkChangedObserver {
    fun onNetConnected(networkInfo: NetworkInfo, networkInfos: List<NetworkInfo>)
    fun onNetDisconnected(network: Network, networkInfos: List<NetworkInfo>)
    fun onCapabilitiesChanged(network: Network, networkInfos: List<NetworkInfo>)
}