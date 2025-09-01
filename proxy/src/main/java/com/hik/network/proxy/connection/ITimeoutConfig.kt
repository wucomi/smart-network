package com.hik.network.proxy.connection

import com.hik.network.proxy.Route

interface ITimeoutConfig {
    fun getConnectTimeoutMs(route: com.hik.network.proxy.Route): Int
}