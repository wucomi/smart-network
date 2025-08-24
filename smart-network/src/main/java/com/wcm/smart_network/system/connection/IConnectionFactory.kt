package com.wcm.smart_network.system.connection

import com.wcm.smart_network.system.Route

interface IConnectionFactory {
    @Throws(Exception::class)
    fun createConnection(route: Route): Connection
}