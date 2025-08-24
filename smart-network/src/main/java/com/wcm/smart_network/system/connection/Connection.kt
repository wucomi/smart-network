package com.wcm.smart_network.system.connection

import com.wcm.smart_network.system.Route
import com.wcm.smart_network.system.network.NetworkInfo
import java.net.Socket

class Connection(
    val socket: Socket,
    var idleAtNs: Long,
    val route: Route,
    val networkInfo: NetworkInfo?,
) {
    // 检查连接是否有效
    fun isHealthy(): Boolean {
        val nowNs = System.nanoTime()
        if (socket.isClosed ||
            socket.isClosed ||
            socket.isInputShutdown ||
            socket.isOutputShutdown
        ) {
            return false
        }
        val idleDurationNs = synchronized(this) { nowNs - idleAtNs }
        if (idleDurationNs >= IDLE_CONNECTION_HEALTHY_NS) {
            return try {
                // 发送紧急数据探活（不影响正常数据）
                socket.sendUrgentData(0xFF)
                true
            } catch (e: Exception) {
                false
            }
        }
        return true
    }

    companion object {
        private const val IDLE_CONNECTION_HEALTHY_NS = 10_000_000_000 // 10 seconds.
    }
}
