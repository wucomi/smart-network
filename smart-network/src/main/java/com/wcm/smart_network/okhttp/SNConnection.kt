package com.wcm.smart_network.okhttp

import java.net.Socket

data class SNConnection(
    val networkInfo: NetworkInfo,
    val socket: Socket
)