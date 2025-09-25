package com.hik.smartnetwork.network

interface INetworkFinder {
    fun findNetwork(address: String?): NetworkInfo?
    fun changeNetwork(address: String)
    fun removeNetwork(address: String)
}