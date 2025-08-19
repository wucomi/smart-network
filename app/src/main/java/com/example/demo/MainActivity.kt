package com.example.demo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.wcm.smart_network.DiskCacheHostNetwork
import com.wcm.smart_network.NetWorkObserver
import com.wcm.smart_network.NetworkType
import com.wcm.smart_network.smartNetwork
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initSmartNetwork()
    }

    private fun initSmartNetwork() {
        NetWorkObserver.init(this.applicationContext)
        val diskCacheHostNetwork = DiskCacheHostNetwork(this.applicationContext)
        val okHttpClient = OkHttpClient.Builder()
            .smartNetwork(NetWorkObserver)
            .setDiskCacheHostNetwork(diskCacheHostNetwork)
            .setStrategy(arrayListOf(NetworkType.Cellular, NetworkType.ExtranetWifi))
            .build()

        okHttpClient.newCall(Request.Builder().url("http://192.168.124.94:8000/").build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("=================onFailure: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    println(("=================onResponse: " + response.body?.string()))
                }
            })
    }
}