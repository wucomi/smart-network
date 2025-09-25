package com.hik.smartnetwork.network

import java.net.Socket

data class NConnection(
    val networkInfo: NetworkInfo,
    val socket: Socket
)