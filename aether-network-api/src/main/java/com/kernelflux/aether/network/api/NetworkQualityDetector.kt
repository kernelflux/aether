package com.kernelflux.aether.network.api

/**
 * 网络质量指标
 */
data class NetworkMetrics(
    /**
     * 往返时延（RTT），单位：毫秒
     */
    val rtt: Long = 0L,
    
    /**
     * 带宽（估算），单位：bps
     */
    val bandwidth: Long = 0L,
    
    /**
     * 丢包率，0.0-1.0
     */
    val packetLossRate: Double = 0.0,
    
    /**
     * 网络类型
     */
    val networkType: NetworkType = NetworkType.UNKNOWN,
    
    /**
     * 信号强度（WiFi/移动网络），0-100
     */
    val signalStrength: Int = 0,
    
    /**
     * 连接质量评分，0-100
     */
    val qualityScore: Int = 0
)

/**
 * 网络类型
 */
enum class NetworkType {
    /**
     * 未知
     */
    UNKNOWN,
    
    /**
     * WiFi
     */
    WIFI,
    
    /**
     * 2G
     */
    G2,
    
    /**
     * 3G
     */
    G3,
    
    /**
     * 4G/LTE
     */
    G4,
    
    /**
     * 5G
     */
    G5,
    
    /**
     * 以太网
     */
    ETHERNET
}

/**
 * 网络质量检测器
 * 用于检测网络质量指标，为弱网优化提供依据
 */
interface NetworkQualityDetector {
    /**
     * 检测当前网络质量
     * @return 网络质量指标
     */
    fun detect(): NetworkMetrics
    
    /**
     * 检测网络质量（异步）
     * @param callback 回调
     */
    fun detectAsync(callback: (NetworkMetrics) -> Unit)
    
    /**
     * 判断是否为弱网
     * @param metrics 网络质量指标
     * @return 是否为弱网
     */
    fun isWeakNetwork(metrics: NetworkMetrics): Boolean
    
    /**
     * 获取网络质量等级
     * @param metrics 网络质量指标
     * @return 网络质量等级
     */
    fun getNetworkQuality(metrics: NetworkMetrics): NetworkQuality
    
    /**
     * 开始持续监控网络质量
     * @param interval 检测间隔（毫秒）
     * @param callback 质量变化回调
     */
    fun startMonitoring(interval: Long, callback: (NetworkMetrics) -> Unit)
    
    /**
     * 停止监控
     */
    fun stopMonitoring()
}

/**
 * 弱网判断标准
 */
data class WeakNetworkThreshold(
    /**
     * RTT 阈值（毫秒），超过此值认为是弱网
     */
    val rttThreshold: Long = 1000L,
    
    /**
     * 带宽阈值（bps），低于此值认为是弱网
     */
    val bandwidthThreshold: Long = 100_000L, // 100 Kbps
    
    /**
     * 丢包率阈值，超过此值认为是弱网
     */
    val packetLossThreshold: Double = 0.1, // 10%
    
    /**
     * 质量评分阈值，低于此值认为是弱网
     */
    val qualityScoreThreshold: Int = 50
)
