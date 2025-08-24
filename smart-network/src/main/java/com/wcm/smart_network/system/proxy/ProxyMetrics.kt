package com.wcm.smart_network.system.proxy

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 代理服务器监控指标
 */
class ProxyMetrics {
    private val totalConnections = AtomicInteger(0)
    private val activeConnections = AtomicInteger(0)
    private val bytesTransferred = AtomicLong(0)
    private val errorCount = AtomicInteger(0)

    fun onConnectionEstablished() {
        totalConnections.incrementAndGet()
        activeConnections.incrementAndGet()
    }

    fun onConnectionClosed() {
        activeConnections.decrementAndGet()
    }

    fun onBytesTransferred(bytes: Int) {
        bytesTransferred.addAndGet(bytes.toLong())
    }

    fun onError() {
        errorCount.incrementAndGet()
    }

    fun getMetricsSnapshot(): Map<String, Number> {
        return ConcurrentHashMap<String, Number>().apply {
            put("total_connections", totalConnections.get())
            put("active_connections", activeConnections.get())
            put("bytes_transferred", bytesTransferred.get())
            put("error_count", errorCount.get())
        }
    }
}
