package com.wcm.smart_network.system.proxy

import com.wcm.smart_network.okhttp.utils.Logger
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * 本地回环代理服务器
 */
class LocalProxyServer(
    private val port: Int = 8888,
    private val protocolHandler: ProtocolHandler,
) {
    private var serverSocket: ServerSocket? = null
    private val metrics = ProxyMetrics()
    private val threadFactory = object : ThreadFactory {
        private val threadNumber = AtomicInteger(1)
        override fun newThread(r: Runnable) = Thread(r, "Proxy-Worker-${threadNumber.incrementAndGet()}")
            .apply { isDaemon = true }
    }
    private val executor = Executors.newCachedThreadPool(threadFactory)

    @get:Synchronized
    @set:Synchronized
    private var isRunning = false

    /**
     * 启动代理服务器
     */
    fun start() {
        if (isRunning) return
        isRunning = true

        Thread {
            try {
                val serverSocket = ServerSocket(
                    port, 50, InetAddress.getByName("127.0.0.1")
                )
                this.serverSocket = serverSocket
                Logger.info("代理服务器启动，端口: $port")

                while (isRunning && !serverSocket.isClosed) {
                    val clientSocket = serverSocket.accept()
                    metrics.onConnectionEstablished()
                    executor.submit {
                        try {
                            protocolHandler.handleClient(clientSocket)
                        } finally {
                            metrics.onConnectionClosed()
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) Logger.error("服务器异常终止", e)
            }
        }.start()
    }

    /**
     * 停止代理服务器
     */
    fun stop() {
        if (!isRunning) return
        isRunning = false
        serverSocket?.closeQuietly()
        executor.shutdown()
        Logger.info("代理服务器已停止，指标: ${metrics.getMetricsSnapshot()}")
    }

    private fun ServerSocket.closeQuietly() {
        try {
            if (!isClosed) close()
        } catch (e: Exception) {
            Logger.warn("关闭ServerSocket异常", e)
        }
    }
}
