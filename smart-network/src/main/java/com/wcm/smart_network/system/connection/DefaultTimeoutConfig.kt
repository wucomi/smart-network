package com.wcm.smart_network.system.connection

import com.wcm.smart_network.system.Route

class DefaultTimeoutConfig : ITimeoutConfig {
    override fun getConnectTimeoutMs(route: Route): Int {
        return 10000
    }
}