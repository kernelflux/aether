package com.kernelflux.aether.network.api

/**
 * 请求优化策略
 */
interface RequestOptimizer {
    /**
     * 优化请求
     * @param request 原始请求
     * @param metrics 网络质量指标
     * @param deviceInfo 设备信息
     * @return 优化后的请求
     */
    fun optimize(
        request: Request,
        metrics: NetworkMetrics,
        deviceInfo: DeviceInfo
    ): Request
    
    /**
     * 是否应该压缩请求体
     * @param request 请求
     * @param metrics 网络质量指标
     */
    fun shouldCompressRequest(request: Request, metrics: NetworkMetrics): Boolean
    
    /**
     * 是否应该压缩响应
     * @param request 请求
     * @param metrics 网络质量指标
     */
    fun shouldCompressResponse(request: Request, metrics: NetworkMetrics): Boolean
    
    /**
     * 获取优化后的超时时间
     * @param request 请求
     * @param metrics 网络质量指标
     * @param defaultTimeout 默认超时时间
     * @return 优化后的超时时间
     */
    fun getOptimizedTimeout(
        request: Request,
        metrics: NetworkMetrics,
        defaultTimeout: Long
    ): Long
    
    /**
     * 是否应该降级请求（减少数据量）
     * @param request 请求
     * @param metrics 网络质量指标
     */
    fun shouldDegradeRequest(request: Request, metrics: NetworkMetrics): Boolean
    
    /**
     * 降级请求（减少返回数据量）
     * @param request 原始请求
     * @return 降级后的请求
     */
    fun degradeRequest(request: Request): Request
    
    /**
     * 是否应该合并请求
     * @param requests 请求列表
     * @param metrics 网络质量指标
     */
    fun shouldBatchRequests(
        requests: List<Request>,
        metrics: NetworkMetrics
    ): Boolean
    
    /**
     * 合并请求
     * @param requests 请求列表
     * @return 合并后的请求
     */
    fun batchRequests(requests: List<Request>): Request?
}

/**
 * 压缩算法
 */
enum class CompressionAlgorithm {
    /**
     * Gzip 压缩
     */
    GZIP,
    
    /**
     * Brotli 压缩
     */
    BROTLI,
    
    /**
     * Deflate 压缩
     */
    DEFLATE,
    
    /**
     * 不压缩
     */
    NONE
}

/**
 * 请求优先级
 */
enum class RequestPriority {
    /**
     * 低优先级
     */
    LOW,
    
    /**
     * 普通优先级
     */
    NORMAL,
    
    /**
     * 高优先级
     */
    HIGH,
    
    /**
     * 紧急优先级
     */
    URGENT
}
