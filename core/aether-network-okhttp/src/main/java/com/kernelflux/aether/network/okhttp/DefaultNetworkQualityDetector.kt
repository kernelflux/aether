package com.kernelflux.aether.network.okhttp

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import com.kernelflux.aether.network.api.*

/**
 * 默认网络质量检测器实现
 */
class DefaultNetworkQualityDetector(
    private val context: Context,
    private val threshold: WeakNetworkThreshold = WeakNetworkThreshold()
) : NetworkQualityDetector {
    
    private var isMonitoring = false
    private var monitoringCallback: ((NetworkMetrics) -> Unit)? = null
    
    @SuppressLint("MissingPermission")
    override fun detect(): NetworkMetrics {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkMetrics()
        
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkMetrics()
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkMetrics()
        
        // 检测网络类型
        val networkType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // 检测移动网络类型
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    when (telephonyManager?.dataNetworkType) {
                        TelephonyManager.NETWORK_TYPE_LTE,
                        TelephonyManager.NETWORK_TYPE_NR -> NetworkType.G4
                        TelephonyManager.NETWORK_TYPE_UMTS,
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_HSPAP -> NetworkType.G3
                        TelephonyManager.NETWORK_TYPE_EDGE,
                        TelephonyManager.NETWORK_TYPE_GPRS -> NetworkType.G2
                        else -> NetworkType.UNKNOWN
                    }
                } else {
                    //
                    NetworkType.UNKNOWN
                }
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.UNKNOWN
        }
        
        // 检测信号强度（简化实现）
        val signalStrength = if (networkType == NetworkType.WIFI) {
            // WiFi 信号强度（简化处理）
            75 // 默认值
        } else {
            // 移动网络信号强度（简化处理）
            70 // 默认值
        }
        
        // 估算 RTT（简化实现）
        val rtt = when (networkType) {
            NetworkType.WIFI -> 50L
            NetworkType.G4 -> 100L
            NetworkType.G3 -> 300L
            NetworkType.G2 -> 800L
            else -> 200L
        }
        
        // 估算带宽（简化实现）
        val bandwidth = when (networkType) {
            NetworkType.WIFI -> 10_000_000L // 10 Mbps
            NetworkType.G4 -> 5_000_000L // 5 Mbps
            NetworkType.G3 -> 1_000_000L // 1 Mbps
            NetworkType.G2 -> 100_000L // 100 Kbps
            else -> 1_000_000L
        }
        
        // 计算质量评分
        val qualityScore = calculateQualityScore(rtt, bandwidth, networkType)
        
        return NetworkMetrics(
            rtt = rtt,
            bandwidth = bandwidth,
            packetLossRate = 0.0, // 简化处理
            networkType = networkType,
            signalStrength = signalStrength,
            qualityScore = qualityScore
        )
    }
    
    override fun detectAsync(callback: (NetworkMetrics) -> Unit) {
        // 简化实现：直接调用同步方法
        callback(detect())
    }
    
    override fun isWeakNetwork(metrics: NetworkMetrics): Boolean {
        return metrics.rtt > threshold.rttThreshold ||
               metrics.bandwidth < threshold.bandwidthThreshold ||
               metrics.packetLossRate > threshold.packetLossThreshold ||
               metrics.qualityScore < threshold.qualityScoreThreshold
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
    
    override fun startMonitoring(interval: Long, callback: (NetworkMetrics) -> Unit) {
        isMonitoring = true
        monitoringCallback = callback
        // 简化实现：不实现真正的监控
    }
    
    override fun stopMonitoring() {
        isMonitoring = false
        monitoringCallback = null
    }
    
    private fun calculateQualityScore(rtt: Long, bandwidth: Long, networkType: NetworkType): Int {
        // 根据 RTT、带宽和网络类型计算质量评分
        var score = 100
        
        // RTT 影响（RTT 越大，分数越低）
        when {
            rtt > 1000 -> score -= 40
            rtt > 500 -> score -= 25
            rtt > 200 -> score -= 10
        }
        
        // 带宽影响（带宽越小，分数越低）
        when {
            bandwidth < 100_000 -> score -= 30 // < 100 Kbps
            bandwidth < 500_000 -> score -= 20 // < 500 Kbps
            bandwidth < 1_000_000 -> score -= 10 // < 1 Mbps
        }
        
        // 网络类型影响
        when (networkType) {
            NetworkType.G2 -> score -= 20
            NetworkType.G3 -> score -= 10
            NetworkType.G4 -> score += 0
            NetworkType.WIFI -> score += 5
            else -> score -= 5
        }
        
        return score.coerceIn(0, 100)
    }
}
