package com.hik.network.proxy.proxy

import com.hik.network.proxy.Route
import com.wcm.smart_network.system.connection.IConnectionPool
import com.wcm.smart_network.okhttp.utils.Logger
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.URL

/**
 * 协议处理器类，负责处理客户端的不同类型网络请求，支持HTTP、HTTPS和普通Socket协议。
 */
class ProtocolHandler(
    private val connectionPool: IConnectionPool,
) {
//    fun handleClient(clientSocket: Socket) {
//        var targetSocket: Socket? = null
//        try {
//            val reader = BufferedReader(InputStreamReader(clientSocket.inputStream))
//            val firstLine = reader.readLine() ?: return
//            Logger.debug("收到请求: $firstLine")
//
//            // 解析请求类型和目标地址
//            val soTimeout = runCatching {
//                clientSocket.soTimeout
//            }.getOrNull()?.takeIf { it > 0 } ?: SO_TIMEOUT_MS
//            if (firstLine.startsWith("CONNECT")) {
//                // 处理HTTPS请求
//                val targetHostPort = firstLine.split(" ")[1]
//                val (host, port) = splitHostPort(targetHostPort, 443)
//                targetSocket = connectionPool.getConnection(Route(host, port)).socket
//                targetSocket.soTimeout = soTimeout
//                // 发送隧道建立响应
//                val writer = BufferedWriter(OutputStreamWriter(clientSocket.outputStream))
//                writer.write("HTTP/1.1 200 Connection Established\r\n\r\n")
//                writer.flush()
//
//                // 转发加密数据
//                forwardData(clientSocket, targetSocket)
//            } else {
//                // 处理HTTP请求（修复404核心逻辑）
//                val firstLineParts = firstLine.split(" ", limit = 3)
//                if (firstLineParts.size != 3) {
////                    sendErrorResponse(clientSocket, 400, "无效的HTTP请求格式")
//                    return
//                }
//                val (method, fullUrl, version) = firstLineParts
//
//                // 解析URL获取目标主机、端口和相对路径
//                val uri = URL(fullUrl)
//                val host = uri.host
//                val port = if (uri.port == -1) 80 else uri.port
//                val requestPath = buildString {
//                    append(uri.path.ifEmpty { "/" }) // 路径为空时补"/"
//                    uri.query?.takeIf { it.isNotEmpty() }?.let { append("?$it") } // 拼接查询参数
//                }
//
//                // 建立目标服务器连接
//                targetSocket = connectionPool.getConnection(Route(host, port)).socket
//                targetSocket.soTimeout = soTimeout
//                // 转发HTTP请求头
//                val requestWriter = BufferedWriter(OutputStreamWriter(targetSocket.outputStream))
//                val realFirstLine = "$method $requestPath $version\r\n"
//                Logger.debug("realFirstLine: $realFirstLine")
//                requestWriter.write(realFirstLine)
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
//            Logger.error("处理客户端异常: ${e.message}", e)
//            // TODO:
//        } finally {
//            clientSocket.closeQuietly()
//            targetSocket?.closeQuietly()
//        }
//    }

    fun handleClient(clientSocket: Socket) {
        var targetSocket: Socket? = null
        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.inputStream))
            val firstLine = reader.readLine() ?: return
            Logger.debug("收到请求: $firstLine")

            // 解析请求类型和目标地址
            val soTimeout = runCatching {
                clientSocket.soTimeout
            }.getOrNull()?.takeIf { it > 0 } ?: SO_TIMEOUT_MS

            if (firstLine.startsWith("CONNECT ")) {
                // 处理HTTPS（CONNECT隧道）
                val (host, port) = parseHttpsFirstLine(firstLine) ?: run {
                    sendError(clientSocket, 400, "无效的HTTPS请求")
                    return
                }
                val connection = runCatching {
                    connectionPool.getConnection(com.hik.network.proxy.Route(host, port))
                }.getOrNull()
                if (connection == null) {
                    sendError(clientSocket, 502, "无法连接到 $host:$port")
                    return
                }
                targetSocket = connection.socket
                targetSocket.soTimeout = soTimeout
                // 发送隧道建立响应
                BufferedWriter(OutputStreamWriter(clientSocket.outputStream)).apply {
                    write("HTTP/1.1 200 Connection Established\r\n\r\n")
                    flush()
                }
                // 转发加密数据
                forwardData(clientSocket, targetSocket)
                // 放回连接池复用
                connectionPool.releaseConnection(connection)
            } else {
                val headerBuffer = ByteArrayOutputStream()
                val bufferedWriter = BufferedWriter(OutputStreamWriter(headerBuffer))
                // 处理HTTP（GET/POST/PATCH等）
                val (method, fullPath, version) = parseHttpFirstLine(firstLine) ?: run {
                    sendError(clientSocket, 400, "无效的HTTP请求")
                    return
                }
                val uri = URL(fullPath)
                val path = buildString {
                    append(uri.path.ifEmpty { "/" }) // 路径为空时补"/"
                    uri.query?.takeIf { it.isNotEmpty() }?.let { append("?$it") } // 拼接查询参数
                }
                bufferedWriter.write("$method $path $version\r\n")

                var hostPort: String? = null
                var contentLength = 0
                var isChunked = false
                var line: String?

                // 解析头
                while (reader.readLine().also { line = it } != null) {
                    bufferedWriter.write("$line\r\n")
                    if (line.isNullOrEmpty()) break
                    when {
                        line!!.startsWith("Host:", ignoreCase = true) -> {
                            hostPort = line!!.substring(5).trim()
                        }

                        line!!.startsWith("Content-Length:", ignoreCase = true) -> {
                            contentLength = line!!.substring(14).trim().toIntOrNull() ?: 0
                        }

                        line!!.startsWith("Transfer-Encoding:", ignoreCase = true) -> {
                            isChunked = line!!.substring(18).trim().equals("chunked", ignoreCase = true)
                        }
                    }
                }
                bufferedWriter.write("\r\n")
                bufferedWriter.flush()

                val (host, port) = parseHttpHost(hostPort, fullPath) ?: run {
                    sendError(clientSocket, 400, "缺少Host头")
                    return
                }

                val connection = runCatching {
                    connectionPool.getConnection(com.hik.network.proxy.Route(host, port))
                }.getOrNull()
                if (connection == null) {
                    sendError(clientSocket, 502, "无法连接到 $host:$port")
                    return
                }

                targetSocket = connection.socket
                targetSocket.soTimeout = soTimeout

                // 转发HTTP请求
                val targetWriter = BufferedWriter(OutputStreamWriter(targetSocket.outputStream))
                val headerBufferedReader = headerBuffer.toByteArray().inputStream().reader().buffered()
                forwardHttpHeaders(headerBufferedReader, targetWriter)
                if (isChunked) {
                    forwardChunkedData(reader, targetWriter)
                } else if (contentLength > 0) {
                    forwardFixedLengthData(reader, targetWriter, contentLength)
                }
                // 转发响应
                forwardData(targetSocket, clientSocket)
                // 放回连接池复用
                println("1111=================11111111111111111212111111")
                connectionPool.releaseConnection(connection)
            }
        } catch (e: Exception) {
            Logger.error("处理客户端异常: ${e.message}", e)
        }
    }

    // 双向转发数据
    private fun forwardData(clientSocket: Socket, targetSocket: Socket) {
        // 客户端 -> 目标服务器
        try {
            clientSocket.inputStream.copyTo(targetSocket.outputStream, 8192)
        } catch (e: Exception) {
            // 正常关闭会抛出异常，忽略
        }
    }

    // 转发HTTP头
    private fun forwardHttpHeaders(reader: BufferedReader, targetWriter: BufferedWriter) {
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            targetWriter.write("$line\r\n")
            if (line.isNullOrEmpty()) break
        }
        targetWriter.write("\r\n")
        targetWriter.flush()
    }

    // 分块传输处理
    private fun forwardChunkedData(reader: BufferedReader, targetWriter: BufferedWriter) {
        val buffer = CharArray(8192)
        while (true) {
            val chunkSizeLine = reader.readLine() ?: break
            targetWriter.write("$chunkSizeLine\r\n")
            val chunkSize = chunkSizeLine.trim().toIntOrNull(16) ?: 0
            if (chunkSize == 0) break

            var remaining = chunkSize
            while (remaining > 0) {
                val read = reader.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                targetWriter.write(String(buffer, 0, read))
                remaining -= read
            }
            targetWriter.write("\r\n") // 块结束标记
            targetWriter.flush()
        }
        targetWriter.write("\r\n") // 分块结束
        targetWriter.flush()
    }

    // 转发固定长度数据
    private fun forwardFixedLengthData(reader: BufferedReader, targetWriter: BufferedWriter, contentLength: Int) {
        val buffer = CharArray(8192)
        var remaining = contentLength
        while (remaining > 0) {
            val read = reader.read(buffer, 0, minOf(buffer.size, remaining))
            if (read <= 0) break
            targetWriter.write(String(buffer, 0, read))
            remaining -= read
        }
        targetWriter.flush()
    }

    // 解析HTTPS首行
    private fun parseHttpsFirstLine(firstLine: String): Pair<String, Int>? {
        val parts = firstLine.split(" ")
        if (parts.size != 3) return null
        val hostPort = parts[1].split(":")
        return if (hostPort.size == 2) {
            Pair(hostPort[0], hostPort[1].toIntOrNull() ?: 443)
        } else null
    }

    // 解析HTTP首行
    private fun parseHttpFirstLine(firstLine: String): Triple<String, String, String>? {
        val parts = firstLine.split(" ")
        return if (parts.size == 3) {
            Triple(parts[0], parts[1], parts[2])
        } else null
    }

    // 从HTTP头中解析Host和端口
    private fun parseHttpHost(hostPort: String?, fullPath: String): Pair<String, Int>? {
        if (hostPort != null) {
            val parts = hostPort.split(":")
            return if (parts.size == 2) {
                Pair(parts[0], parts[1].toInt())
            } else {
                Pair(hostPort, 80)
            }
        } else {
            val uri = URL(fullPath)
            val host = uri.host
            val port = if (uri.port == -1) 80 else uri.port
            return Pair(host, port)
        }
    }

    // 发送HTTP错误响应
    private fun sendError(socket: Socket, code: Int, message: String) {
        try {
            val response = """
                HTTP/1.1 $code ${if (code == 400) "Bad Request" else "Bad Gateway"}
                Content-Length: ${message.length}
                Connection: close

                $message
            """.trimIndent()
            socket.outputStream.write(response.toByteArray())
            socket.outputStream.flush()
        } catch (e: Exception) { /* 忽略错误 */
        }
    }

    companion object {
        const val SO_TIMEOUT_MS = 30_000
    }
}
