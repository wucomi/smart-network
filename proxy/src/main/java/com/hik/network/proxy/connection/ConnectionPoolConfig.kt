package com.hik.network.proxy.connection

import java.util.concurrent.TimeUnit

data class ConnectionPoolConfig(
    val maxIdleConnections: Int, // 每个host最大空闲连接数
    val keepAliveDuration: Long, // 连接空闲超时
    val timeUnit: TimeUnit, // 连接空闲超时单位
)
