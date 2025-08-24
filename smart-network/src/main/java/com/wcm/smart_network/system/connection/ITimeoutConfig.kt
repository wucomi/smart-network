package com.wcm.smart_network.system.connection

import com.wcm.smart_network.system.Route

interface ITimeoutConfig {
    fun getConnectTimeoutMs(route: Route): Int
}