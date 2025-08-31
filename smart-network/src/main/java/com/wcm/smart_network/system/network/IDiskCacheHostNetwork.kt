package com.wcm.smart_network.system.network

interface IDiskCacheHostNetwork {
    fun getAddressNetwork(address: String): Long
    fun putAddressNetwork(address: String, networkHandle: Long)
    fun removeAddressNetwork(address: String)
    fun clear()
}