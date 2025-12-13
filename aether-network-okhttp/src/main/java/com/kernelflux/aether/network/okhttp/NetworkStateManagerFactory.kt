package com.kernelflux.aether.network.okhttp

import android.content.Context
import com.kernelflux.aether.network.api.NetworkStateManager

/**
 * 网络状态管理器工厂
 * 用于创建网络状态管理器实例
 *
 * 整个模块只用于 Android 平台，直接使用 Android 实现
 */
object NetworkStateManagerFactory {
    /**
     * 创建网络状态管理器
     *
     * @param context Android Context
     * @return 网络状态管理器实例
     */
    @JvmStatic
    fun create(context: Context): NetworkStateManager {
        return DefaultNetworkStateManager(context)
    }
}
