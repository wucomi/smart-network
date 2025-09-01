package com.hik.network.proxy.network

interface INetworkFinder {
    fun findNetwork(address: String): NetworkInfo?
    fun putNetwork(address: String, networkInfo: NetworkInfo)
    fun changeNetwork(address: String)
    fun removeNetwork(address: String)
    fun getSortNetworks(address: String): List<NetworkInfo>
    fun clear()
}