package com.hik.smartnetwork.url

import com.hik.smartnetwork.SmartNetwork
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

class SmartNetworkURLStreamHandler : URLStreamHandler() {
    private val finder = SmartNetwork.finder
    override fun openConnection(url: URL): URLConnection {
        return finder.findNetwork("${url.host}:${url.port}")?.network?.openConnection(url)
            ?: url.openConnection()
    }
}