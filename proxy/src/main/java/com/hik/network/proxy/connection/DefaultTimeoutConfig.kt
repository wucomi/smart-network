package com.hik.network.proxy.connection

import com.hik.network.proxy.Route

class DefaultTimeoutConfig : ITimeoutConfig {
    override fun getConnectTimeoutMs(route: com.hik.network.proxy.Route): Int {
        return 10000
    }
}