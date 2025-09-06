package com.example.demo

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.wcm.smart_network.okhttp.network.NetworkType
import com.wcm.smart_network.okhttp.smartNetwork
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class Process2Service : Service() {
    private var connectivityManager: ConnectivityManager? = null
    private var network2: Network? = null

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR) // 绑定到 Cellular 网络
            .build()

        connectivityManager!!.requestNetwork(request, object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityManager!!.bindProcessToNetwork(network)
                network2 = network
                Log.d("Process2Service", "Bound to network: $network")
                initSmartNetwork()
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val url = "http://www.kuaidi100.com/query?type=快递公司代号&postid=快递单号"
    private fun initSmartNetwork() {
        val sslContext = SSLContext.getInstance("TLS")
        val trustManagers = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls<X509Certificate>(0)
            }
        })
        sslContext.init(null, trustManagers, SecureRandom())

        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustManagers[0] as X509TrustManager)
//            .proxySelector(object : ProxySelector() {
//                override fun select(uri: java.net.URI?): MutableList<java.net.Proxy> {
//                    return mutableListOf(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 8888)))
//                }
//
//                override fun connectFailed(uri: java.net.URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {
//
//                }
//
//            })
            .smartNetwork()
            .setStrategy(arrayListOf(NetworkType.Cellular, NetworkType.ExtranetWifi))
            .build()

        okHttpClient.newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Process2Service=================onFailure: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    val string = response.body?.string()
                    println(("Process2Service=================onResponse: " + string))
                }
            })
    }
}