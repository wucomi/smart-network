package com.hik.network.proxy.connection

import com.hik.network.proxy.Route

interface IConnectionFactory {
    @Throws(Exception::class)
    fun createConnection(route: com.hik.network.proxy.Route): Connection
}