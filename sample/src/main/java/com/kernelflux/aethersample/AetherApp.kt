package com.kernelflux.aethersample

import android.app.Application
import android.content.Context
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.network.api.INetworkClient
import com.kernelflux.fluxrouter.core.FluxRouter

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
        networkClient?.init(
            this,
            com.kernelflux.aether.network.api.NetworkConfig.builder()
                .baseUrl("https://api.github.com/")
                .connectTimeout(30_000)
                .readTimeout(30_000)
                .writeTimeout(30_000)
                .enableLogging(true)
                .enableCookies(true)
                .build()
        )

        // 4. 配置日志（示例）
        FluxRouter.getService(ILogger::class.java)?.apply {
            setLogLevel(com.kernelflux.aether.log.api.LogLevel.DEBUG)
            setEnabled(true)
        }
    }
}