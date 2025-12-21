package com.kernelflux.aether.network.okhttp

import com.kernelflux.aether.network.api.*

/**
 * 默认弱网处理器实现
 * 结合网络质量检测、设备性能检测和 ROM 兼容性处理
 */
class DefaultWeakNetworkHandler(
    private val qualityDetector: NetworkQualityDetector,
    private val deviceDetector: DevicePerformanceDetector,
    private val requestOptimizer: RequestOptimizer,
    private val romHandler: RomCompatibilityHandler? = null
) : WeakNetworkHandler {
    
    private var cachedMetrics: NetworkMetrics? = null
    private var cachedDeviceInfo: DeviceInfo? = null
    private var isWeakNetworkCached: Boolean? = null
    
    override fun isWeakNetwork(): Boolean {
        if (isWeakNetworkCached != null) {
            return isWeakNetworkCached!!
        }
        
        val metrics = getNetworkMetrics()
        val isWeak = qualityDetector.isWeakNetwork(metrics)
        isWeakNetworkCached = isWeak
        return isWeak
    }
    
    override fun getNetworkQuality(): Int {
        return getNetworkMetrics().qualityScore
    }
    
    override fun getNetworkMetrics(): NetworkMetrics {
        if (cachedMetrics == null) {
            cachedMetrics = qualityDetector.detect()
        }
        return cachedMetrics!!
    }
    
    override fun optimizeRequest(request: Request): Request {
        val metrics = getNetworkMetrics()
        val deviceInfo = getDeviceInfo()
        
        // 使用请求优化器优化请求
        var optimizedRequest = requestOptimizer.optimize(request, metrics, deviceInfo)
        
        // ROM 特定优化
        if (romHandler != null) {
            optimizedRequest = romHandler.handleBackgroundRestriction(
                optimizedRequest,
                deviceInfo.isBackground
            )
        }
        
        // 添加压缩头
        if (shouldCompressRequest()) {
            val algorithm = getCompressionAlgorithm()
            optimizedRequest = optimizedRequest.copy(
                headers = optimizedRequest.headers + mapOf(
                    "Accept-Encoding" to when (algorithm) {
                        CompressionAlgorithm.GZIP -> "gzip, deflate"
                        CompressionAlgorithm.BROTLI -> "br, gzip, deflate"
                        CompressionAlgorithm.DEFLATE -> "deflate"
                        CompressionAlgorithm.NONE -> ""
                    }
                )
            )
        }
        
        // 调整超时时间
        if (shouldReduceTimeout()) {
            val timeout = getWeakNetworkTimeout()
            optimizedRequest = optimizedRequest.copy(timeout = timeout)
        }
        
        return optimizedRequest
    }
    
    override fun shouldCompressRequest(): Boolean {
        val metrics = getNetworkMetrics()
        return requestOptimizer.shouldCompressRequest(
            Request(url = ""), // 占位请求
            metrics
        )
    }
    
    override fun shouldCompressResponse(): Boolean {
        val metrics = getNetworkMetrics()
        return requestOptimizer.shouldCompressResponse(
            Request(url = ""), // 占位请求
            metrics
        )
    }
    
    override fun getCompressionAlgorithm(): CompressionAlgorithm {
        val deviceInfo = getDeviceInfo()
        
        // 弱网且非低端设备，使用 Brotli（压缩率高但 CPU 消耗大）
        if (isWeakNetwork() && !deviceInfo.isLowEndDevice) {
            return CompressionAlgorithm.BROTLI
        }
        
        // 弱网且低端设备，使用 Gzip（平衡压缩率和 CPU）
        if (isWeakNetwork()) {
            return CompressionAlgorithm.GZIP
        }
        
        // 正常网络，不压缩或使用 Gzip
        return CompressionAlgorithm.GZIP
    }
    
    override fun shouldReduceTimeout(): Boolean {
        return isWeakNetwork()
    }
    
    override fun getWeakNetworkTimeout(): Long {
        val metrics = getNetworkMetrics()
        val defaultTimeout = 30_000L // 30秒
        
        return requestOptimizer.getOptimizedTimeout(
            Request(url = ""), // 占位请求
            metrics,
            defaultTimeout
        )
    }
    
    override fun shouldBatchRequests(): Boolean {
        val metrics = getNetworkMetrics()
        // 弱网环境下建议合并请求
        return isWeakNetwork() && metrics.bandwidth < 500_000L // 500 Kbps
    }
    
    override fun shouldDegradeRequest(): Boolean {
        val metrics = getNetworkMetrics()
        return requestOptimizer.shouldDegradeRequest(
            Request(url = ""), // 占位请求
            metrics
        )
    }
    
    override fun getRequestPriority(request: Request): RequestPriority {
        val deviceInfo = getDeviceInfo()
        
        // 低端设备或后台，降低优先级
        if (deviceInfo.isLowEndDevice || deviceInfo.isBackground) {
            return RequestPriority.LOW
        }
        
        // 弱网环境下，根据请求重要性调整优先级
        if (isWeakNetwork()) {
            // 可以根据请求的 tag 或其他属性判断重要性
            return RequestPriority.NORMAL
        }
        
        return RequestPriority.NORMAL
    }
    
    override fun shouldUseHttp2(): Boolean {
        val metrics = getNetworkMetrics()
        val deviceInfo = getDeviceInfo()
        
        // HTTP/2 多路复用对弱网有帮助，但需要设备支持
        return !deviceInfo.isLowEndDevice && metrics.networkType != NetworkType.G2
    }
    
    override fun shouldReuseConnection(): Boolean {
        // 连接复用对弱网有帮助
        return true
    }
    
    override fun getSuggestedConnectionPoolSize(): Int {
        val deviceInfo = getDeviceInfo()
        
        // 低端设备使用较小的连接池
        if (deviceInfo.isLowEndDevice) {
            return 3
        }
        
        // 中端设备
        if (deviceInfo.isMidRangeDevice) {
            return 5
        }
        
        // 高端设备
        return 10
    }
    
    override fun shouldPreResolveDns(): Boolean {
        // 弱网环境下，DNS 预解析可以减少延迟
        return isWeakNetwork()
    }
    
    override fun getDeviceInfo(): DeviceInfo {
        if (cachedDeviceInfo == null) {
            cachedDeviceInfo = deviceDetector.detectDeviceInfo()
        }
        return cachedDeviceInfo!!
    }
    
    override fun shouldOptimizeForDevice(): Boolean {
        val deviceInfo = getDeviceInfo()
        return deviceInfo.isLowEndDevice || deviceInfo.isMidRangeDevice
    }
    
    /**
     * 刷新缓存（当网络状态变化时调用）
     */
    fun refreshCache() {
        cachedMetrics = null
        cachedDeviceInfo = null
        isWeakNetworkCached = null
    }
}
