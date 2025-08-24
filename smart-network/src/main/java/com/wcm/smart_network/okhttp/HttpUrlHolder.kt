package com.wcm.smart_network.okhttp

import okhttp3.HttpUrl

class HttpUrlHolder {
    private val threadLocalHost = ThreadLocal<HttpUrl>()

    fun setUrl(host: HttpUrl) {
        threadLocalHost.set(host)
    }

    fun getUrl(): HttpUrl? {
        return threadLocalHost.get()
    }

    fun getAddress(): String? {
        return threadLocalHost.get()?.let {
            "${it.host}:${it.port}"
        }
    }

    fun clear() {
        threadLocalHost.remove()
    }
}