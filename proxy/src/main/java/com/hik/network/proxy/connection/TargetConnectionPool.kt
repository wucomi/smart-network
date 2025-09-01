package com.hik.network.proxy.connection

import com.hik.network.proxy.Route
import okhttp3.internal.closeQuietly
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TargetConnectionPool(
    private val config: ConnectionPoolConfig,
    private val connectionFactory: IConnectionFactory,
) : IConnectionPool {
    // 存储连接: key=host:port，value=空闲连接队列（带过期时间）
    private val connections = ConcurrentHashMap<String, LinkedList<Connection>>()

    // 定时清理过期连接（每30秒执行一次）
    private val cleaner = Executors.newSingleThreadScheduledExecutor()
    private val keepAliveDurationNs: Long = config.timeUnit.toNanos(config.keepAliveDuration)
    private val maxIdleConnections: Int = config.maxIdleConnections

    init {
        cleaner.scheduleWithFixedDelay(
            { cleanExpiredConnections() },
            30, 30, TimeUnit.SECONDS
        )
    }

    // 清理过期连接
    private fun cleanExpiredConnections() {
        val now = System.nanoTime()
        connections.forEach { (key, queue) ->
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val connection = iterator.next()
                if (now - connection.idleAtNs > keepAliveDurationNs || !connection.isHealthy()) {
                    connection.socket.closeQuietly()
                    iterator.remove()
                }
            }
            if (queue.isEmpty()) {
                connections.remove(key)
            }
        }
    }

    // 从连接池获取连接，若无则新建
    @Throws(Exception::class)
    override fun getConnection(route: com.hik.network.proxy.Route): Connection {
        val now = System.nanoTime()
        val key = "${route.host}:${route.port}"
        // 尝试从队列中获取可用连接
        connections[key]?.let { queue ->
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val connection = iterator.next()
                if (now - connection.idleAtNs > keepAliveDurationNs || !connection.isHealthy()) {
                    connection.socket.closeQuietly()
                    iterator.remove()
                } else {
                    return connection
                }
            }
            if (queue.isEmpty()) {
                connections.remove(key)
            }
        }

        // 无可用连接，新建连接
        return createNewConnection(route)
    }

    // 创建新连接
    @Throws(Exception::class)
    private fun createNewConnection(route: com.hik.network.proxy.Route): Connection {
        return connectionFactory.createConnection(route)
    }

    // 释放连接回池（空闲时复用）
    override fun releaseConnection(connection: Connection) {
        if (!connection.isHealthy()) return

        val key = "${connection.route.host}:${connection.route.port}"
        val queue = connections.getOrPut(key) { LinkedList() }
        // 若队列未满，添加到池；否则直接关闭
        if (queue.size < maxIdleConnections) {
            connection.idleAtNs = System.nanoTime()
            queue.add(connection)
        } else {
            connection.socket.closeQuietly()
        }
    }

    // 关闭连接池
    override fun shutdown() {
        cleaner.shutdown()
        connections.values.forEach { queue ->
            queue.forEach { it.socket.closeQuietly() }
        }
        connections.clear()
    }
}