package com.wcm.smart_network.system

data class Route(
    val host: String,
    val port: Int,
) {
    val address: String
        get() = "$host:$port"
}
