package com.hik.network.proxy.proxy

import com.wcm.smart_network.okhttp.utils.Logger

class GlobalProxy {
    fun startGlobalProxy(port: String) {
        try {
            // 设置全局HTTP代理
            System.setProperty("http.proxyHost", "127.0.0.1")
            System.setProperty("http.proxyPort", port)
            // 设置全局HTTPS代理
            System.setProperty("https.proxyHost", "127.0.0.1")
            System.setProperty("https.proxyPort", port)
            // 设置全局SOCKS代理
            System.setProperty("socks.proxyHost", "127.0.0.1")
            System.setProperty("socks.proxyPort", port)
//            // 设置全局FTP代理
//            System.setProperty("ftp.proxyHost", "127.0.0.1")
//            System.setProperty("ftp.proxyPort", port)
//            // 设置全局代理
//            System.setProperty("proxySet", "true")
            Logger.debug("setGlobalProxy: success")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopGlobalProxy() {
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")
        System.clearProperty("https.proxyHost")
        System.clearProperty("https.proxyPort")
        System.clearProperty("socks.proxyHost")
        System.clearProperty("socks.proxyPort")
//        System.clearProperty("ftp.proxyHost")
//        System.clearProperty("ftp.proxyPort")
//        System.clearProperty("proxySet")
    }
}