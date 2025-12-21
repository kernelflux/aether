package com.kernelflux.aethersample

import android.app.Application
import android.content.Context
import com.kernelflux.aether.log.api.FileConfig
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LogLevel
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

        // 初始化日志系统（推荐在 Application 中显式初始化）
        val logger = FluxRouter.getService(com.kernelflux.aether.log.api.ILogger::class.java)
        logger?.init(
            context = this,
            defaultConfig = LoggerConfig(
                level = LogLevel.DEBUG, // 示例中使用 DEBUG，生产环境建议使用 INFO
                consoleEnabled = true, // 示例中启用控制台输出
                fileEnabled = true,
                fileConfig = FileConfig(
                    logDir = File(filesDir, "logs").absolutePath,
                    cacheDir = File(cacheDir, "log_cache").absolutePath,
                    namePrefix = "aether_sample",
                    maxFileSize = 10 * 1024 * 1024, // 10MB
                    maxAliveTime = 7 * 24 * 60 * 60 * 1000L, // 7天
                    cacheDays = 3, // 缓存日志保留3天
                    compressEnabled = false, // 开发时禁用压缩，方便直接查看日志文件
                    customHeaderInfo = try {
                        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName
                        mapOf<String, String>(
                            "Device" to (android.os.Build.MODEL ?: "Unknown"),
                            "Manufacturer" to (android.os.Build.MANUFACTURER ?: "Unknown"),
                            "Android Version" to (android.os.Build.VERSION.RELEASE ?: "Unknown"),
                            "SDK Version" to android.os.Build.VERSION.SDK_INT.toString(),
                            "App Version" to (appVersion ?: "Unknown")
                            // 可以添加更多信息，如 IP 地址等
                            // "IP Address" to getLocalIpAddress()
                        )
                    } catch (e: Exception) {
                        mapOf<String, String>(
                            "Device" to (android.os.Build.MODEL ?: "Unknown"),
                            "Manufacturer" to (android.os.Build.MANUFACTURER ?: "Unknown"),
                            "Android Version" to (android.os.Build.VERSION.RELEASE ?: "Unknown"),
                            "SDK Version" to android.os.Build.VERSION.SDK_INT.toString()
                        )
                    }
                )
            )
        )

        // 配置网络客户端（示例）
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
    }

    // 注意：现在使用Android标准的Resources系统
    // 资源文件在 res/values/strings.xml 中定义
    // 多语言资源在 res/values-zh-rCN/strings.xml 等目录中定义
    // 不再需要ResourceManager，直接使用Context.getString()即可
}
