package com.kernelflux.aether.network.api

/**
 * 弱网处理器
 * 用于在弱网环境下优化网络请求
 */
interface WeakNetworkHandler {
    /**
     * 检测是否为弱网环境
     * @return 是否为弱网
     */
    fun isWeakNetwork(): Boolean
    
    /**
     * 获取网络质量等级
     * @return 网络质量（0-100，0 最差，100 最好）
     */
    fun getNetworkQuality(): Int
    
    /**
     * 获取网络质量指标
     * @return 网络质量指标
     */
    fun getNetworkMetrics(): NetworkMetrics
    
    /**
     * 在弱网环境下的请求优化策略
     * @param request 原始请求
     * @return 优化后的请求
     */
    fun optimizeRequest(request: Request): Request
    
    /**
     * 是否应该压缩请求体
     */
    fun shouldCompressRequest(): Boolean
    
    /**
     * 是否应该压缩响应
     */
    fun shouldCompressResponse(): Boolean
    
    /**
     * 获取压缩算法
     */
    fun getCompressionAlgorithm(): CompressionAlgorithm
    
    /**
     * 是否应该使用更小的超时时间
     */
    fun shouldReduceTimeout(): Boolean
    
    /**
     * 获取弱网环境下的超时时间（毫秒）
     */
    fun getWeakNetworkTimeout(): Long
    
    /**
     * 是否应该启用请求合并
     */
    fun shouldBatchRequests(): Boolean
    
    /**
     * 是否应该降级请求（减少数据量）
     */
    fun shouldDegradeRequest(): Boolean
    
    /**
     * 获取请求优先级
     * @param request 请求
     * @return 请求优先级
     */
    fun getRequestPriority(request: Request): RequestPriority
    
    /**
     * 是否应该使用 HTTP/2
     */
    fun shouldUseHttp2(): Boolean
    
    /**
     * 是否应该启用连接复用
     */
    fun shouldReuseConnection(): Boolean
    
    /**
     * 获取连接池大小建议
     */
    fun getSuggestedConnectionPoolSize(): Int
    
    /**
     * 是否应该启用 DNS 预解析
     */
    fun shouldPreResolveDns(): Boolean
    
    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): DeviceInfo
    
    /**
     * 是否应该针对设备性能进行优化
     */
    fun shouldOptimizeForDevice(): Boolean
}

/**
 * 网络质量等级
 */
enum class NetworkQuality {
    /**
     * 无网络
     */
    NONE,
    
    /**
     * 极差（2G）
     */
    POOR,
    
    /**
     * 较差（3G）
     */
    FAIR,
    
    /**
     * 良好（4G）
     */
    GOOD,
    
    /**
     * 优秀（5G/WiFi）
     */
    EXCELLENT
}
