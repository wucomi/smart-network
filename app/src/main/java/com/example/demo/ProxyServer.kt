//package com.example.demo
//
//import android.util.Log
//import java.io.BufferedReader
//import java.io.BufferedWriter
//import java.io.IOException
//import java.io.InputStreamReader
//import java.io.OutputStreamWriter
//import java.net.InetAddress
//import java.net.InetSocketAddress
//import java.net.ServerSocket
//import java.net.Socket
//import java.net.URL
//import java.util.concurrent.Executors
//import kotlin.concurrent.thread
//
//class ProxyServer(private val port: Int = 8888) {
//    private var serverSocket: ServerSocket? = null
//    private val executor = Executors.newCachedThreadPool()
//    private var isRunning = false
//
//    fun start() {
//        isRunning = true
//        Thread {
//            try {
//                // 绑定本地回环地址，仅允许应用内访问
//                serverSocket = ServerSocket(port, 100, InetAddress.getByName("127.0.0.1"))
//                Log.d("ProxyServer", "服务器启动成功，监听端口: $port")
//
//                while (isRunning) {
//                    // 接收客户端连接
//                    val clientSocket = serverSocket?.accept() ?: break
//                    executor.submit { handleClient(clientSocket) }
//                }
//            } catch (e: Exception) {
//                if (isRunning) {
//                    Log.e("ProxyServer", "服务器异常: ${e.message}", e)
//                }
//            }
//        }.start()
//    }
//
//    fun stop() {
//        isRunning = false
//        serverSocket?.close()
//        executor.shutdown()
//    }
//
//    private fun handleClient(clientSocket: Socket) {
//        var targetSocket: Socket? = null
//        try {
//            clientSocket.soTimeout = 10000 // 10秒超时
//            val reader = BufferedReader(InputStreamReader(clientSocket.inputStream))
//            val firstLine = reader.readLine() ?: return
//            Log.d("ProxyServer", "收到请求: $firstLine")
//
//            // 解析请求类型和目标地址
//            if (firstLine.startsWith("CONNECT")) {
//                // 处理HTTPS请求
//                // 建立目标服务器连接
//                targetSocket = Socket()
//                targetSocket.connect(InetSocketAddress(clientSocket.inetAddress, clientSocket.port), clientSocket.soTimeout)
//
//                // 发送连接建立响应
//                val writer = BufferedWriter(OutputStreamWriter(clientSocket.outputStream))
//                writer.write("HTTP/1.1 200 Connection Established\r\n\r\n")
//                writer.flush()
//
//                // 转发加密数据
//                forwardData(clientSocket, targetSocket)
//            } else {
//                // 处理HTTP请求
//                val url = firstLine.split(" ")[1]
//                val uri = URL(url)
//                val host = uri.host
//                val port = if (uri.port == -1) 80 else uri.port
//
//                // 建立目标服务器连接
//                targetSocket = Socket(host, port)
//
//                // 转发HTTP请求头
//                val requestWriter = BufferedWriter(OutputStreamWriter(targetSocket.outputStream))
//                requestWriter.write("$firstLine\r\n")
//
//                // 转发剩余请求头
//                var line: String?
//                while (reader.readLine().also { line = it } != null) {
//                    if (line.isNullOrEmpty()) break
//                    requestWriter.write("$line\r\n")
//                }
//                requestWriter.write("\r\n")
//                requestWriter.flush()
//
//                // 转发数据
//                forwardData(clientSocket, targetSocket)
//            }
//        } catch (e: Exception) {
//            Log.e("ProxyServer", "处理客户端异常: ${e.message}", e)
//        } finally {
//            clientSocket.closeQuietly()
//            targetSocket?.closeQuietly()
//        }
//    }
//
//    // 双向转发数据
//    private fun forwardData(clientSocket: Socket, targetSocket: Socket) {
//        // 客户端 -> 目标服务器
//        thread {
//            try {
//                clientSocket.inputStream.copyTo(targetSocket.outputStream, 8192)
//            } catch (e: Exception) {
//                // 正常关闭会抛出异常，忽略
//            }
//        }
//
//        // 目标服务器 -> 客户端
//        thread {
//            try {
//                targetSocket.inputStream.copyTo(clientSocket.outputStream, 8192)
//            } catch (e: Exception) {
//                // 正常关闭会抛出异常，忽略
//            }
//        }.join() // 等待转发完成
//    }
//
//    // 分割主机和端口
//    private fun splitHostPort(hostPort: String, defaultPort: Int): Pair<String, Int> {
//        val parts = hostPort.split(":", limit = 2)
//        return if (parts.size == 2) {
//            Pair(parts[0], parts[1].toIntOrNull() ?: defaultPort)
//        } else {
//            Pair(parts[0], defaultPort)
//        }
//    }
//
//    // 安全关闭Socket
//    private fun Socket.closeQuietly() {
//        try {
//            close()
//        } catch (e: IOException) {
//            // 忽略关闭异常
//        }
//    }
//}