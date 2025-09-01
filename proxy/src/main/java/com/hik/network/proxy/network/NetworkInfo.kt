package com.hik.network.proxy.network

import android.net.Network

data class NetworkInfo(
    val network: Network,
    val networkType: NetworkType,
)
