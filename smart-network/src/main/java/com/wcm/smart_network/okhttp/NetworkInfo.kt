package com.wcm.smart_network.okhttp

import android.net.Network

data class NetworkInfo(
    val network: Network,
    val networkType: NetworkType,
)
