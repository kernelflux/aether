package com.kernelflux.aethersample

import android.app.Application
import android.content.Context
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.network.api.INetworkClient
import com.kernelflux.fluxrouter.core.FluxRouter
import java.io.File

/**
 * Aether框架示例应用
 *
 * @author Aether Framework
 */
class AetherApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化Aether框架（会初始化所有已注册的服务）
        FluxRouter.init(this)

        // 3. 配置网络客户端（示例）
        val networkClient = FluxRouter.getService(INetworkClient::class.java)
        if (networkClient == null) {
            android.util.Log.e("AetherApp", "NetworkClient service not found! Make sure aether-network-okhttp module is included.")
        } else {
            android.util.Log.d("AetherApp", "NetworkClient instance: ${networkClient.hashCode()}")
            try {
                // 配置缓存目录（可选，如果不设置则使用应用缓存目录）
                val cacheDir = File(cacheDir, "aether_network")
                
                networkClient.init(
                    this,
                    com.kernelflux.aether.network.api.NetworkConfig.builder()
                        .baseUrl("https://api.github.com/")
                        .connectTimeout(30_000)
                        .readTimeout(30_000)
                        .writeTimeout(30_000)
                        .enableLogging(true)
                        .enableCookies(true)
                        .cacheDir(cacheDir)  // 设置缓存目录
                        .build()
                )
                android.util.Log.d("AetherApp", "NetworkClient initialized successfully, instance: ${networkClient.hashCode()}")
            } catch (e: Exception) {
                android.util.Log.e("AetherApp", "Failed to initialize NetworkClient", e)
            }
        }

        // 4. 配置日志（示例）
        FluxRouter.getService(ILogger::class.java)?.apply {
            setLogLevel(com.kernelflux.aether.log.api.LogLevel.DEBUG)
            setEnabled(true)
        }
    }
}