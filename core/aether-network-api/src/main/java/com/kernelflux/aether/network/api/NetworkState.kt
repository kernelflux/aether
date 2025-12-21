package com.kernelflux.aether.network.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 网络状态
 */
enum class NetworkState {
    /**
     * 无网络
     */
    NONE,
    
    /**
     * 有网络
     */
    AVAILABLE,
    
    /**
     * 弱网
     */
    WEAK
}

/**
 * 网络状态监听器
 */
interface NetworkStateListener {
    /**
     * 网络状态变化回调
     * @param oldState 旧状态
     * @param newState 新状态
     */
    fun onNetworkStateChanged(oldState: NetworkState, newState: NetworkState)
}

/**
 * 默认网络状态监听器实现
 * 提供健壮的默认实现，避免回调异常导致崩溃
 */
open class DefaultNetworkStateListener(
    private val onStateChanged: (NetworkState, NetworkState) -> Unit = { _, _ -> }
) : NetworkStateListener {
    
    override fun onNetworkStateChanged(oldState: NetworkState, newState: NetworkState) {
        try {
            onStateChanged(oldState, newState)
        } catch (e: Exception) {
            // 异常处理，避免崩溃
            // 可以记录日志，但不抛异常
        }
    }
}

/**
 * 网络策略
 * 用于处理无网场景、无网恢复有网等场景的切换优化
 */
interface NetworkStrategy {
    /**
     * 获取当前网络状态
     * @return 当前网络状态
     */
    fun getCurrentNetworkState(): NetworkState
    
    /**
     * 处理无网场景
     * @param request 请求
     * @return 处理结果（是否应该继续请求、是否使用缓存等）
     */
    fun handleNoNetwork(request: Request): NoNetworkResult
    
    /**
     * 处理网络恢复场景
     * @param pendingRequests 待处理的请求列表
     */
    fun handleNetworkRestored(pendingRequests: List<Request>)
    
    /**
     * 是否应该缓存请求（在无网时）
     */
    fun shouldCacheRequest(request: Request): Boolean
    
    /**
     * 是否应该自动重试（在网络恢复后）
     */
    fun shouldAutoRetry(request: Request): Boolean
}

/**
 * 无网场景处理结果
 */
sealed class NoNetworkResult {
    /**
     * 使用缓存
     */
    object UseCache : NoNetworkResult()
    
    /**
     * 加入待处理队列（网络恢复后重试）
     */
    object QueueRequest : NoNetworkResult()
    
    /**
     * 直接失败
     */
    object Fail : NoNetworkResult()
    
    /**
     * 自定义处理
     */
    data class Custom(val action: () -> Unit) : NoNetworkResult()
}

/**
 * 默认网络策略实现
 * 提供健壮的默认实现，结合网络状态管理器
 * 
 * 特性：
 * - 自动感知网络状态变化
 * - 待处理请求队列管理
 * - 完善的异常处理
 */
class DefaultNetworkStrategy(
    private val networkStateManager: NetworkStateManager? = null
) : NetworkStrategy {
    
    // 待处理的请求队列（网络恢复后重试）
    private val pendingRequests = ConcurrentHashMap<String, Request>()
    
    override fun getCurrentNetworkState(): NetworkState {
        return try {
            // 优先使用 NetworkStateManager 获取状态
            networkStateManager?.getCurrentState() 
                ?: NetworkState.AVAILABLE
        } catch (e: Exception) {
            // 异常处理，返回默认状态，不抛异常
            NetworkState.AVAILABLE
        }
    }
    
    override fun handleNoNetwork(request: Request): NoNetworkResult {
        return try {
            // 如果有缓存策略，优先使用缓存
            if (request.cacheStrategy != null && 
                request.cacheStrategy != CacheStrategy.NO_CACHE) {
                NoNetworkResult.UseCache
            } else {
                // 加入待处理队列
                val requestKey = generateRequestKey(request)
                pendingRequests[requestKey] = request
                NoNetworkResult.QueueRequest
            }
        } catch (e: Exception) {
            // 异常处理，返回失败而不是抛异常
            NoNetworkResult.Fail
        }
    }
    
    override fun handleNetworkRestored(pendingRequests: List<Request>) {
        try {
            // 网络恢复后，可以自动重试待处理的请求
            // 这里只是保存请求，实际重试由调用方决定
            pendingRequests.forEach { request ->
                val requestKey = generateRequestKey(request)
                this.pendingRequests[requestKey] = request
            }
        } catch (e: Exception) {
            // 异常处理，避免崩溃
        }
    }
    
    override fun shouldCacheRequest(request: Request): Boolean {
        return try {
            request.cacheStrategy != null && 
            request.cacheStrategy != CacheStrategy.NO_CACHE
        } catch (e: Exception) {
            false
        }
    }
    
    override fun shouldAutoRetry(request: Request): Boolean {
        return try {
            request.retryStrategy != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取待处理的请求列表
     */
    fun getPendingRequests(): List<Request> {
        return try {
            pendingRequests.values.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 清除待处理的请求
     */
    fun clearPendingRequests() {
        try {
            pendingRequests.clear()
        } catch (e: Exception) {
            // 异常处理
        }
    }
    
    /**
     * 移除已处理的请求
     */
    fun removePendingRequest(request: Request) {
        try {
            val requestKey = generateRequestKey(request)
            pendingRequests.remove(requestKey)
        } catch (e: Exception) {
            // 异常处理
        }
    }
    
    private fun generateRequestKey(request: Request): String {
        return "${request.method}:${request.url}:${request.tag}"
    }
    
    companion object {
        /**
         * 创建默认实例（不依赖网络状态管理器）
         */
        fun create(): DefaultNetworkStrategy {
            return DefaultNetworkStrategy(null)
        }
    }
}
