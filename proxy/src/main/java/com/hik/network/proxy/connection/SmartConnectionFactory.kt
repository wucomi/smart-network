package com.hik.network.proxy.connection

import android.os.Looper
import com.hik.network.proxy.Route
import com.wcm.smart_network.system.network.INetworkFinder
import com.wcm.smart_network.system.network.NetworkInfo
import com.wcm.smart_network.okhttp.utils.Logger
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import javax.net.SocketFactory

class SmartConnectionFactory(
    private val finder: INetworkFinder,
    private val timeoutConfig: ITimeoutConfig,
    private val fastConnect: Boolean,
) : IConnectionFactory {
    private val executorService = Executors.newCachedThreadPool()

    @Throws(Exception::class)
    override fun createConnection(route: com.hik.network.proxy.Route): Connection {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalStateException("Cannot find network on the main thread.")
        }
        val network = finder.findNetwork(route.address)
        if (network == null) {
            return createConnectionWithSniff(route)
        } else {
            val socketFactory = getSocketFactory(network)
            val socket = socketFactory.createSocket()
            socket.connect(
                InetSocketAddress(route.host, route.port),
                timeoutConfig.getConnectTimeoutMs(route)
            )
            return Connection(socket, System.nanoTime(), route, network)
        }
    }

    @Throws(Exception::class)
    private fun createConnectionWithSniff(route: com.hik.network.proxy.Route): Connection {
        // 通过检测网络连通性获取
        var networkInfo: NetworkInfo? = null
        var successSocket: Socket? = null
        if (fastConnect) {
            // 快速匹配
            val sockets = Collections.synchronizedCollection(mutableListOf<Socket>())
            val countDownLatch = CountDownLatch(2)
            finder.getSortNetworks(route.address)
                .onEach {
                    val socket = getSocketFactory(it).createSocket()
                    sockets.add(socket)
                    executorService.execute {
                        if (isReachable(
                                socket,
                                route.host,
                                route.port,
                                timeoutConfig.getConnectTimeoutMs(route)
                            )
                        ) {
                            networkInfo = it
                            successSocket = socket
                            (0 until countDownLatch.count).forEach { _ ->
                                countDownLatch.countDown()
                            }
                        } else {
                            countDownLatch.countDown()
                        }
                    }
                }
            countDownLatch.await()
            // 将其他连接断开, 避免占用线程
            sockets.filter { it != successSocket }.forEach {
                it.closeQuietly()
            }
        } else {
            networkInfo = finder.getSortNetworks(route.address).find {
                val socket = getSocketFactory(it).createSocket()
                val isReachable = isReachable(
                    socket,
                    route.host,
                    route.port,
                    timeoutConfig.getConnectTimeoutMs(route)
                )
                if (isReachable) {
                    successSocket = socket
                } else {
                    socket.closeQuietly()

                }
                isReachable
            }
        }
        networkInfo?.apply {
            Logger.debug("Find Network from check reachable: network=${this}")
            finder.putNetwork(route.address, this)
        }
        successSocket?.apply {
            return Connection(this, System.nanoTime(), route, networkInfo)
        }
        throw IOException("Create connection failed")
    }

    private fun getSocketFactory(networkInfo: NetworkInfo?): SocketFactory {
        return networkInfo?.network?.socketFactory ?: SocketFactory.getDefault()
    }

    private fun isReachable(socket: Socket, host: String?, port: Int, timeoutMs: Int) = runCatching {
        if (host.isNullOrBlank() || host == "null") {
            socket.connect(
                InetSocketAddress(port),
                timeoutMs,
            )
        } else {
            socket.connect(
                InetSocketAddress(host, port),
                timeoutMs,
            )
        }
    }.isSuccess
}