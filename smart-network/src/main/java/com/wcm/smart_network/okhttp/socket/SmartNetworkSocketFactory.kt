package com.hik.smartnetwork.okhttp.socket

import com.hik.smartnetwork.okhttp.network.INetworkFinder
import com.hik.smartnetwork.okhttp.network.NetworkInfo
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

internal class SmartNetworkSocketFactory(
    private val urlHolder: HttpUrlHolder,
    private val finder: INetworkFinder,
) : SocketFactory() {
    override fun createSocket(): Socket {
        val network = finder.findNetwork(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket()
    }

    override fun createSocket(host: String?, port: Int): Socket {
        val network = finder.findNetwork(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket(host, port)
    }

    override fun createSocket(
        host: String?, port: Int,
        localHost: InetAddress?, localPort: Int
    ): Socket {
        val network = finder.findNetwork(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket(host, port, localHost, localPort)
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        val network = finder.findNetwork(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket(host, port)
    }

    override fun createSocket(
        address: InetAddress?, port: Int,
        localAddress: InetAddress?, localPort: Int
    ): Socket {
        val network = finder.findNetwork(urlHolder.getAddress())
        val socketFactory = getSocketFactory(network)
        return socketFactory.createSocket(address, port, localAddress, localPort)
    }

    private fun getSocketFactory(networkInfo: NetworkInfo?): SocketFactory {
        return networkInfo?.let {
            if (it.isVpn) {
                getDefault()
            } else {
                it.network.socketFactory
            }
        } ?: getDefault()
    }
}