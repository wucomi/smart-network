package com.wcm.smart_network.system.network

import android.net.Network

data class NetworkInfo(
    val network: Network,
    val networkType: NetworkType,
)
