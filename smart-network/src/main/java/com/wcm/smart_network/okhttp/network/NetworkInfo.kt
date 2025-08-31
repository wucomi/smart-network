package com.wcm.smart_network.okhttp.network

import android.net.Network
import android.net.NetworkCapabilities

data class NetworkInfo(
    val network: Network,
    val networkType: NetworkType,
    val isVpn: Boolean,
    val capabilities: NetworkCapabilities?,
)
