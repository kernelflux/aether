package com.kernelflux.aether.network.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 网络状态管理器
 * 负责网络状态的检测、监听和管理
 * 
 * 注意：这是接口定义，具体实现应该在实现模块中提供
 */
interface NetworkStateManager {
    /**
     * 获取当前网络状态
     * @return 当前网络状态
     */
    fun getCurrentState(): NetworkState
    
    /**
     * 获取网络状态 Flow
     * 使用 Kotlin Flow 提供响应式网络状态
     * @return 网络状态 Flow
     */
    fun getNetworkStateFlow(): Flow<NetworkState>
    
    /**
     * 获取网络状态 StateFlow（如果支持）
     * StateFlow 会保留最新状态，适合 UI 观察
     * @return 网络状态 StateFlow，如果不支持则返回 null
     */
    fun getNetworkStateStateFlow(): StateFlow<NetworkState>
    
    /**
     * 添加网络状态监听器
     * @param listener 监听器
     */
    fun addListener(listener: NetworkStateListener)
    
    /**
     * 移除网络状态监听器
     * @param listener 监听器
     */
    fun removeListener(listener: NetworkStateListener)
    
    /**
     * 开始监听网络状态变化
     */
    fun startMonitoring()
    
    /**
     * 停止监听网络状态变化
     */
    fun stopMonitoring()
    
    /**
     * 检查网络是否可用
     * @return 网络是否可用
     */
    fun isNetworkAvailable(): Boolean
    
    /**
     * 检查是否为弱网
     * @return 是否为弱网
     */
    fun isWeakNetwork(): Boolean
}
