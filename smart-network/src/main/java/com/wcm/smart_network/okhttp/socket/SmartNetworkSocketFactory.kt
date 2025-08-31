package com.wcm.smart_network.okhttp.socket

import com.wcm.smart_network.okhttp.network.NetworkFinder
import com.wcm.smart_network.okhttp.network.NetworkInfo
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

internal class SmartNetworkSocketFactory(
    private val urlHolder: HttpUrlHolder,
    private val finder: NetworkFinder,
) : SocketFactory() {
    override fun createSocket(): Socket {
        val network = finder.find(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket()
    }

    override fun createSocket(host: String?, port: Int): Socket {
        val network = finder.find(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket(host, port)
    }

    override fun createSocket(
        host: String?, port: Int,
        localHost: InetAddress?, localPort: Int
    ): Socket {
        val network = finder.find(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket(host, port, localHost, localPort)
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        val network = finder.find(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket(host, port)
    }

    override fun createSocket(
        address: InetAddress?, port: Int,
        localAddress: InetAddress?, localPort: Int
    ): Socket {
        val network = finder.find(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket(address, port, localAddress, localPort)
    }

    private fun getSocketFactory(networkInfo: NetworkInfo?): SocketFactory {
        return networkInfo?.network?.socketFactory ?: getDefault()
    }
}