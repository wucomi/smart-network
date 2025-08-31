package com.example.demo

import android.content.ContentValues
import android.content.Context
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.wcm.smart_network.okhttp.network.NetworkType
import com.wcm.smart_network.okhttp.smartNetwork
import com.wcm.smart_network.system.SmartNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread


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

//        GlobalProxy().startGlobalProxy("8888")
//        SimpleProxyServer().start()
        SmartNetwork.init(application) {
            port = 8888
        }

//        thread {
//            val socket = Socket("192.168.124.94", 8888)
//            println("======================23333333")
//            socket.getOutputStream().write("12121211".toByteArray())
//            println("======================12121211")
//        }
//        thread {
//            val socket = Socket()
//            socket.connect(InetSocketAddress("192.168.124.94", 80), 10000)
//            println("======================23333333")
//            socket.getOutputStream().write("23333333".toByteArray())
//            println("======================23333333")
//        }
        Handler(Looper.getMainLooper()).postDelayed({
            val pic =
                "https://ts1.tc.mm.bing.net/th/id/R-C.32af73b7aea55367e4a3f8763961c894?rik=QUOZ37Lf%2bE7rbw&riu=http%3a%2f%2fimg95.699pic.com%2fphoto%2f50064%2f7148.jpg_wh860.jpg&ehk=00VUYvoTnuinFTJXQYbpLwh3e%2bLPJE9vL7h5ELm0avA%3d&risl=&pid=ImgRaw&r=0"
            Glide.with(this).asFile().load(pic).into(object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    println("======================${resource.absolutePath}")

                    val galleryUri = addToGallery(resource, "img_${System.currentTimeMillis()}.jpg")
                    galleryUri?.let {
                        // 通知 MediaScanner 立即刷新（可选，Android 10+ 不需要）
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            MediaScannerConnection.scanFile(
                                this@MainActivity,
                                arrayOf(it.toString()),
                                arrayOf("image/jpeg"),
                                null
                            )
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })

            initSmartNetwork()
        }, 0)

        lifecycleScope.launch {
            println("开始执行，当前时间: ${System.currentTimeMillis()}")
            fdf()
            println("最终结果")
        }
    }

    private suspend fun fdf(){
        val result = withTimeoutOrNull(1000) { // 设定1秒超时

            // 启动一个IO密集型任务
            val deferred = async(Dispatchers.IO) {
                // 尝试连接一个不存在的IP:端口（会触发长时间阻塞）
                val socket = Socket()
                try {
                    // 注意：这里的超时参数是Socket自身的连接超时
                    socket.connect(InetSocketAddress("192.168.255.255", 8080), 5000)
                    "连接成功"
                } catch (e: Exception) {
                    "连接失败: ${e.message}"
                } finally {
                    socket.close()
                }
            }

            deferred.await() // 等待IO任务结果
        }

        // 实际执行时间会远超1秒
        println("超时结束，当前时间: ${System.currentTimeMillis()}")
    }

    private val url = "http://192.168.124.94:8000"
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
                    println("=================onFailure: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    val string = response.body?.string()
                    val substring = string?.substring(0..10)
                    println("===================$substring")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this@MainActivity, "$substring", Toast.LENGTH_SHORT).show()
                    }
                    println(("=================onResponse: " + string))
                }
            })
    }
}

/**
 * 把 Glide 下载到的文件插入相册
 * @param file      Glide 返回的原始文件
 * @param displayName  在相册中显示的文件名（不含扩展名）
 * @param mimeType     如 image/jpeg、image/png
 * @return 相册中的 Uri
 */
fun Context.addToGallery(
    file: File,
    displayName: String,
    mimeType: String = "image/jpeg"
): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
    }

    val resolver = contentResolver
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val uri = resolver.insert(collection, values) ?: return null

    // 拷贝文件内容
    resolver.openOutputStream(uri)?.use { out ->
        file.inputStream().use { input ->
            input.copyTo(out)
        }
    }
    return uri
}

class SimpleProxyServer(private val port: Int = 8888) {
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var isRunning = false

    fun start() {
        isRunning = true
        Thread {
            try {
                // 绑定本地回环地址，仅允许应用内访问
                serverSocket = ServerSocket(port, 100, InetAddress.getByName("127.0.0.1"))
                Log.d("ProxyServer", "服务器启动成功，监听端口: $port")

                while (isRunning) {
                    // 接收客户端连接
                    val clientSocket = serverSocket?.accept() ?: break
                    executor.submit { handleClient(clientSocket) }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("ProxyServer", "服务器异常: ${e.message}", e)
                }
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        executor.shutdown()
    }

    private fun handleClient(clientSocket: Socket) {
        var targetSocket: Socket? = null
        try {
            clientSocket.soTimeout = 10000 // 10秒超时
            val reader = BufferedReader(InputStreamReader(clientSocket.inputStream))
            val firstLine = reader.readLine() ?: return
            Log.d("ProxyServer", "收到请求: $firstLine")

            // 解析请求类型和目标地址
            if (firstLine.startsWith("CONNECT")) {
                // 处理HTTPS请求
                val hostPort = firstLine.split(" ")[1]
                val (host, port) = splitHostPort(hostPort, 443)

                // 建立目标服务器连接
                targetSocket = Socket()
                targetSocket.connect(InetSocketAddress(host, port), 5000)

                // 发送连接建立响应
                val writer = BufferedWriter(OutputStreamWriter(clientSocket.outputStream))
                writer.write("HTTP/1.1 200 Connection Established\r\n\r\n")
                writer.flush()

                // 转发加密数据
                forwardData(clientSocket, targetSocket)
            } else {
                // 处理HTTP请求
                val url = firstLine.split(" ")[1]
                val uri = URL(url)
                val host = uri.host
                val port = if (uri.port == -1) 80 else uri.port

                // 建立目标服务器连接
                targetSocket = Socket(host, port)

                // 转发HTTP请求头
                val requestWriter = BufferedWriter(OutputStreamWriter(targetSocket.outputStream))
                requestWriter.write("$firstLine\r\n")

                // 转发剩余请求头
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line.isNullOrEmpty()) break
                    requestWriter.write("$line\r\n")
                }
                requestWriter.write("\r\n")
                requestWriter.flush()

                // 转发数据
                forwardData(clientSocket, targetSocket)
            }
        } catch (e: Exception) {
            Log.e("ProxyServer", "处理客户端异常: ${e.message}", e)
        } finally {
            clientSocket.closeQuietly()
            targetSocket?.closeQuietly()
        }
    }

    // 双向转发数据
    private fun forwardData(clientSocket: Socket, targetSocket: Socket) {
        // 客户端 -> 目标服务器
        thread {
            try {
                clientSocket.inputStream.copyTo(targetSocket.outputStream, 8192)
            } catch (e: Exception) {
                // 正常关闭会抛出异常，忽略
            }
        }

        // 目标服务器 -> 客户端
        thread {
            try {
                targetSocket.inputStream.copyTo(clientSocket.outputStream, 8192)
            } catch (e: Exception) {
                // 正常关闭会抛出异常，忽略
            }
        }.join() // 等待转发完成
    }

    // 分割主机和端口
    private fun splitHostPort(hostPort: String, defaultPort: Int): Pair<String, Int> {
        val parts = hostPort.split(":", limit = 2)
        return if (parts.size == 2) {
            Pair(parts[0], parts[1].toIntOrNull() ?: defaultPort)
        } else {
            Pair(parts[0], defaultPort)
        }
    }

    // 安全关闭Socket
    private fun Socket.closeQuietly() {
        try {
            close()
        } catch (e: IOException) {
            // 忽略关闭异常
        }
    }
}