package com.hik.smartnetwork.okhttp.network

interface INetworkFinder {
    fun findNetwork(address: String?): NetworkInfo?
    fun changeNetwork(address: String)
    fun removeNetwork(address: String)
}