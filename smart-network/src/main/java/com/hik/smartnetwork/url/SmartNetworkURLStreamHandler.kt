package com.hik.smartnetwork.url

import com.hik.smartnetwork.network.INetworkFinder
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

class SmartNetworkURLStreamHandler(
    private val finder: INetworkFinder
) : URLStreamHandler() {
    override fun openConnection(url: URL): URLConnection {
        return finder.findNetwork("${url.host}:${url.port}")?.network?.openConnection(url)
            ?: url.openConnection()
    }
}