package com.hik.network.proxy

data class Route(
    val host: String,
    val port: Int,
) {
    val address: String
        get() = "$host:$port"
}
