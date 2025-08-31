//package com.wcm.smart_network.system
//
//import okhttp3.internal.closeQuietly
//import java.net.InetAddress
//import java.net.InetSocketAddress
//import java.net.ServerSocket
//import java.net.Socket
//import kotlin.concurrent.thread
//
//class LocalProxyServer(
//    private val networkStrategy: NetworkStrategy, // 网络策略管理器（见步骤3）
//    private val port: Int = 8888
//) : Thread() {
//    private var serverSocket: ServerSocket? = null
//    private var isRunning = true
//
//    override fun run() {
//        try {
//            // 绑定回环地址，仅允许应用内访问
//            serverSocket = ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"))
//            while (isRunning) {
//                // 接收客户端连接（应用内所有网络请求）
//                val clientSocket = serverSocket!!.accept()
//                // 启动线程处理单个请求
//                ProxyHandler(clientSocket, networkStrategy).start()
//            }
//        } catch (e: Exception) {
//            if (isRunning) e.printStackTrace()
//        }
//    }
//
//    fun stopServer() {
//        isRunning = false
//        serverSocket?.close()
//    }
//
//    // 处理单个代理请求的线程
//    private class ProxyHandler(
//        private val clientSocket: Socket,
//        private val networkStrategy: NetworkStrategy
//    ) : Thread() {
//        override fun run() {
//            var targetSocket: Socket? = null
//            try {
//                // 步骤1：从客户端请求中解析目标Host和端口
//                val (host, port) = parseTargetHostAndPort(clientSocket)
//                if (host.isNullOrEmpty() || port <= 0) {
//                    clientSocket.close()
//                    return
//                }
//
//                // 步骤2：根据Host匹配目标网络
//                val targetNetwork = networkStrategy.findNetwork(host)
//                if (targetNetwork == null) {
//                    clientSocket.close()
//                    return
//                }
//
//                // 步骤3：创建目标Socket并绑定到匹配的网络
//                targetSocket = Socket()
//                targetNetwork.bindSocket(targetSocket) // 核心：绑定网络
//                targetSocket.connect(InetSocketAddress(host, port), 5000) // 连接目标服务器
//
//                // 步骤4：双向转发数据（客户端 <-> 目标服务器）
//                forwardData(clientSocket, targetSocket)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                clientSocket.closeQuietly()
//                targetSocket?.closeQuietly()
//            }
//        }
//
//        // 解析客户端请求中的目标Host和端口（支持HTTP CONNECT和普通请求）
//        private fun parseTargetHostAndPort(clientSocket: Socket): Pair<String?, Int> {
//            val inputStream = clientSocket.getInputStream().bufferedReader()
//            val firstLine = inputStream.readLine() ?: return Pair(null, -1)
//
//            // 处理HTTP CONNECT方法（HTTPS代理）
//            if (firstLine.startsWith("CONNECT")) {
//                val hostPort = firstLine.split(" ")[1]
//                val parts = hostPort.split(":")
//                return Pair(parts[0], parts[1].toInt())
//            }
//
//            // 处理普通HTTP请求
//            if (firstLine.startsWith(Regex("GET|POST|PUT|DELETE"))) {
//                val hostHeader = inputStream.lineSequence()
//                    .firstOrNull { it.startsWith("Host: ") }
//                    ?.substringAfter("Host: ")
//                val parts = hostHeader?.split(":") ?: return Pair(null, -1)
//                return Pair(parts[0], if (parts.size > 1) parts[1].toInt() else 80)
//            }
//
//            // 处理纯TCP请求（直接使用目标地址）
//            val remoteAddr = clientSocket.remoteSocketAddress as? InetSocketAddress
//            return Pair(remoteAddr?.hostName, remoteAddr?.port ?: -1)
//        }
//
//        // 双向转发数据
//        private fun forwardData(clientSocket: Socket, targetSocket: Socket) {
//            // 客户端 -> 目标服务器
//            thread {
//                clientSocket.getInputStream().copyTo(targetSocket.getOutputStream(), 8192)
//            }
//            // 目标服务器 -> 客户端
//            thread {
//                targetSocket.getInputStream().copyTo(clientSocket.getOutputStream(), 8192)
//            }.join() // 等待转发完成
//        }
//    }
//}