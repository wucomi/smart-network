package com.wcm.smart_network.system.connection

import com.wcm.smart_network.system.Route

interface IConnectionPool {
    /**
     * 从连接池获取连接
     * @param route 路由
     * @return Connection
     */
    @Throws(Exception::class)
    fun getConnection(route: Route): Connection

    /**
     * 释放连接回池
     * @param connection 连接
     */
    fun releaseConnection(connection: Connection)

    /**
     * 关闭连接池
     */
    fun shutdown()
}