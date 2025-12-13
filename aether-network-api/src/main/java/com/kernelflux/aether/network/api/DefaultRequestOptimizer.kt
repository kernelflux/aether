package com.kernelflux.aether.network.api

/**
 * 默认请求优化器实现
 */
class DefaultRequestOptimizer(
    private val weakNetworkThreshold: WeakNetworkThreshold = WeakNetworkThreshold()
) : RequestOptimizer {
    
    override fun optimize(
        request: Request,
        metrics: NetworkMetrics,
        deviceInfo: DeviceInfo
    ): Request {
        var optimizedRequest = request
        
        // 1. 超时时间优化
        val timeout = getOptimizedTimeout(request, metrics, request.timeout ?: 30_000L)
        optimizedRequest = optimizedRequest.copy(timeout = timeout)
        
        // 2. 请求降级
        if (shouldDegradeRequest(request, metrics)) {
            optimizedRequest = degradeRequest(optimizedRequest)
        }
        
        // 3. 添加弱网标识头（可选，用于服务端识别）
        if (qualityDetector.isWeakNetwork(metrics)) {
            optimizedRequest = optimizedRequest.copy(
                headers = optimizedRequest.headers + mapOf(
                    "X-Network-Quality" to metrics.qualityScore.toString(),
                    "X-Device-Performance" to if (deviceInfo.isLowEndDevice) "low" else "normal"
                )
            )
        }
        
        return optimizedRequest
    }
    
    override fun shouldCompressRequest(request: Request, metrics: NetworkMetrics): Boolean {
        // 弱网环境下压缩请求体
        if (qualityDetector.isWeakNetwork(metrics)) {
            // 只压缩较大的请求体
            val bodySize = estimateBodySize(request.body)
            return bodySize > 1024 // 大于 1KB 才压缩
        }
        return false
    }
    
    override fun shouldCompressResponse(request: Request, metrics: NetworkMetrics): Boolean {
        // 弱网环境下总是压缩响应
        return qualityDetector.isWeakNetwork(metrics)
    }
    
    override fun getOptimizedTimeout(
        request: Request,
        metrics: NetworkMetrics,
        defaultTimeout: Long
    ): Long {
        // 弱网环境下增加超时时间
        if (qualityDetector.isWeakNetwork(metrics)) {
            // RTT 越大，超时时间越长
            val rttMultiplier = (metrics.rtt / 100.0).coerceAtLeast(1.0).coerceAtMost(3.0)
            return (defaultTimeout * rttMultiplier).toLong()
        }
        
        // 正常网络，使用默认超时时间
        return defaultTimeout
    }
    
    override fun shouldDegradeRequest(request: Request, metrics: NetworkMetrics): Boolean {
        // 极弱网环境下建议降级
        return metrics.qualityScore < 30 || metrics.bandwidth < 50_000L // 50 Kbps
    }
    
    override fun degradeRequest(request: Request): Request {
        // 添加降级参数，让服务端返回精简数据
        val degradedParams = request.params + mapOf(
            "degraded" to "true",
            "minimal" to "true"
        )
        
        return request.copy(params = degradedParams)
    }
    
    override fun shouldBatchRequests(
        requests: List<Request>,
        metrics: NetworkMetrics
    ): Boolean {
        // 弱网环境下，如果多个请求可以合并，建议合并
        if (!qualityDetector.isWeakNetwork(metrics)) {
            return false
        }
        
        // 只有相同域名的请求才能合并
        val sameDomain = requests.map { extractDomain(it.url) }.distinct().size == 1
        return sameDomain && requests.size >= 2
    }
    
    override fun batchRequests(requests: List<Request>): Request? {
        if (requests.isEmpty()) return null
        
        // 简化实现：只合并 GET 请求
        val getRequests = requests.filter { it.method == HttpMethod.GET }
        if (getRequests.size < 2) return null
        
        // 实际实现中，应该将多个请求合并为一个批量请求
        // 这里简化处理，返回第一个请求
        return getRequests.first()
    }
    
    // ========== 私有方法 ==========
    
    private fun estimateBodySize(body: RequestBody?): Long {
        return when (body) {
            is RequestBody.JsonBody -> {
                // 估算 JSON 大小（简化处理）
                500L
            }
            is RequestBody.RawBody -> {
                body.data.size.toLong()
            }
            is RequestBody.ProtobufBody -> {
                body.data.size.toLong()
            }
            else -> 0L
        }
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private val qualityDetector = object : NetworkQualityDetector {
        override fun detect(): NetworkMetrics = NetworkMetrics()
        override fun detectAsync(callback: (NetworkMetrics) -> Unit) {}
        override fun isWeakNetwork(metrics: NetworkMetrics): Boolean {
            return metrics.rtt > weakNetworkThreshold.rttThreshold ||
                   metrics.bandwidth < weakNetworkThreshold.bandwidthThreshold ||
                   metrics.packetLossRate > weakNetworkThreshold.packetLossThreshold ||
                   metrics.qualityScore < weakNetworkThreshold.qualityScoreThreshold
        }
        override fun getNetworkQuality(metrics: NetworkMetrics): NetworkQuality {
            return when {
                metrics.qualityScore >= 80 -> NetworkQuality.EXCELLENT
                metrics.qualityScore >= 60 -> NetworkQuality.GOOD
                metrics.qualityScore >= 40 -> NetworkQuality.FAIR
                metrics.qualityScore >= 20 -> NetworkQuality.POOR
                else -> NetworkQuality.NONE
            }
        }
        override fun startMonitoring(interval: Long, callback: (NetworkMetrics) -> Unit) {}
        override fun stopMonitoring() {}
    }
}
