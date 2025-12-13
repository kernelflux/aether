package com.kernelflux.aether.network.okhttp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.kernelflux.aether.network.api.NetworkState
import com.kernelflux.aether.network.api.NetworkStateListener
import com.kernelflux.aether.network.api.NetworkStateManager
import com.kernelflux.aether.network.api.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 默认网络状态管理器实现（Android 平台）
 * 基于 ConnectivityManager 提供真实的网络状态检测
 *
 * 特性：
 * - 基于 ConnectivityManager.NetworkCallback（API 21+）
 * - 支持 Kotlin Flow
 * - 自动检测网络类型（WiFi、2G/3G/4G/5G）
 * - 弱网检测（基于信号强度、带宽、网络类型）
 * - 完善的异常处理，防止崩溃
 * - 线程安全
 * - 兼容中低端设备和系统
 */
class DefaultNetworkStateManager(
    context: Context
) : NetworkStateManager {

    private val connectivityManager: ConnectivityManager = try {
        ContextCompat.getSystemService(
            context,
            ConnectivityManager::class.java
        ) ?: throw IllegalStateException("ConnectivityManager not available")
    } catch (e: Exception) {
        // 异常处理：如果无法获取 ConnectivityManager，使用备用方案
        throw IllegalStateException("Failed to initialize ConnectivityManager", e)
    }

    private val listeners = CopyOnWriteArrayList<NetworkStateListener>()
    private val _networkStateFlow = MutableStateFlow<NetworkState>(getCurrentState())
    private val isMonitoring = AtomicBoolean(false)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null


    @SuppressLint("ObsoleteSdkInt", "MissingPermission")
    override fun getCurrentState(): NetworkState {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23+ 使用新 API
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

                if (capabilities == null) {
                    NetworkState.NONE
                } else {
                    determineState(capabilities)
                }
            } else {
                // API < 23 的兼容处理
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                if (networkInfo?.isConnected == true) {
                    // 无法精确判断弱网，默认返回 AVAILABLE
                    NetworkState.AVAILABLE
                } else {
                    NetworkState.NONE
                }
            }
        } catch (_: Exception) {
            // 异常处理，返回默认状态，不抛异常
            NetworkState.AVAILABLE
        }
    }

    override fun getNetworkStateFlow(): Flow<NetworkState> {
        return _networkStateFlow
    }

    override fun getNetworkStateStateFlow(): StateFlow<NetworkState> {
        return _networkStateFlow.asStateFlow()
    }

    override fun addListener(listener: NetworkStateListener) {
        try {
            listeners.add(listener)
        } catch (_: Exception) {
        }
    }

    override fun removeListener(listener: NetworkStateListener) {
        try {
            listeners.remove(listener)
        } catch (_: Exception) {
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun startMonitoring() {
        if (isMonitoring.getAndSet(true)) {
            return // 已经在监听
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // API 24+ 使用 NetworkCallback
                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                    override fun onAvailable(network: Network) {
                        try {
                            val capabilities = connectivityManager.getNetworkCapabilities(network)
                            val state = if (capabilities != null) {
                                determineState(capabilities)
                            } else {
                                NetworkState.AVAILABLE
                            }
                            updateState(state)
                        } catch (_: Exception) {
                            updateState(NetworkState.AVAILABLE)
                        }
                    }

                    override fun onLost(network: Network) {
                        try {
                            updateState(NetworkState.NONE)
                        } catch (_: Exception) {
                        }
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        try {
                            val state = determineState(networkCapabilities)
                            updateState(state)
                        } catch (_: Exception) {
                        }
                    }

                    override fun onUnavailable() {
                        try {
                            updateState(NetworkState.NONE)
                        } catch (_: Exception) {
                        }
                    }

                    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                    override fun onLinkPropertiesChanged(
                        network: Network,
                        linkProperties: LinkProperties
                    ) {
                        try {
                            val capabilities = connectivityManager.getNetworkCapabilities(network)
                            if (capabilities != null) {
                                val state = determineState(capabilities)
                                updateState(state)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build()

                connectivityManager.registerNetworkCallback(request, networkCallback!!)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // API 21-23 使用 NetworkCallback（但功能受限）
                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        try {
                            updateState(NetworkState.AVAILABLE)
                        } catch (_: Exception) {
                        }
                    }

                    override fun onLost(network: Network) {
                        try {
                            updateState(NetworkState.NONE)
                        } catch (_: Exception) {
                        }
                    }
                }.also {
                    val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    connectivityManager.registerNetworkCallback(request, it)
                }
            } else {
                // API < 21，无法使用 NetworkCallback
                // 使用轮询方式（不推荐，但作为降级方案）
                isMonitoring.set(false)
            }
        } catch (_: Exception) {
            isMonitoring.set(false)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun stopMonitoring() {
        if (!isMonitoring.getAndSet(false)) {
            return
        }

        try {
            networkCallback?.let { callback ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
                networkCallback = null
            }
        } catch (_: Exception) {
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun isNetworkAvailable(): Boolean {
        return try {
            getCurrentState() != NetworkState.NONE
        } catch (_: Exception) {
            true
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun isWeakNetwork(): Boolean {
        return try {
            getCurrentState() == NetworkState.WEAK
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 根据网络能力确定网络状态
     * 针对中低端设备和弱网场景优化
     */
    private fun determineState(capabilities: NetworkCapabilities): NetworkState {
        return try {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    // WiFi 网络
                    val signalStrength = getWifiSignalStrength(capabilities)
                    val downlinkKbps = capabilities.linkDownstreamBandwidthKbps

                    // 弱网判断：信号强度 < 30 或带宽 < 1Mbps
                    if (signalStrength < 30 || (downlinkKbps in 1..<1000)) {
                        NetworkState.WEAK
                    } else {
                        NetworkState.AVAILABLE
                    }
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    // 移动网络
                    val networkType = detectCellularType(capabilities)
                    val signalStrength = getCellularSignalStrength(capabilities)
                    val downlinkKbps = capabilities.linkDownstreamBandwidthKbps

                    // 弱网判断：2G/3G 或信号强度 < 20 或带宽 < 500Kbps
                    when {
                        networkType == NetworkType.G2 || networkType == NetworkType.G3 -> {
                            NetworkState.WEAK
                        }

                        signalStrength < 20 -> {
                            NetworkState.WEAK
                        }

                        downlinkKbps in 1..<500 -> {
                            NetworkState.WEAK
                        }

                        else -> {
                            NetworkState.AVAILABLE
                        }
                    }
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    // 以太网，通常质量较好
                    NetworkState.AVAILABLE
                }

                else -> {
                    // 其他类型或无法确定
                    NetworkState.NONE
                }
            }
        } catch (_: Exception) {
            // 异常处理，返回默认状态
            NetworkState.AVAILABLE
        }
    }

    /**
     * 检测移动网络类型
     * 兼容各种 ROM 和系统版本
     */
    private fun detectCellularType(capabilities: NetworkCapabilities): NetworkType {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+ 可以使用更精确的方法
                val signalStrength = capabilities.signalStrength
                val downlinkKbps = capabilities.linkDownstreamBandwidthKbps

                // 结合信号强度和带宽判断
                when {
                    downlinkKbps in 1..<100 -> NetworkType.G2
                    downlinkKbps in 100..<1000 -> NetworkType.G3
                    downlinkKbps in 1000..<10000 -> NetworkType.G4
                    downlinkKbps >= 10000 -> NetworkType.G5
                    signalStrength in 0..10 -> NetworkType.G2
                    signalStrength in 11..20 -> NetworkType.G3
                    signalStrength in 21..30 -> NetworkType.G4
                    else -> NetworkType.G4
                }
            } else {
                // API < 30，使用带宽判断
                val downlinkKbps = capabilities.linkDownstreamBandwidthKbps
                when {
                    downlinkKbps in 1..<100 -> NetworkType.G2
                    downlinkKbps in 100..<1000 -> NetworkType.G3
                    downlinkKbps in 1000..<10000 -> NetworkType.G4
                    downlinkKbps >= 10000 -> NetworkType.G5
                    else -> NetworkType.G4 // 默认 4G
                }
            }
        } catch (_: Exception) {
            NetworkType.UNKNOWN
        }
    }

    /**
     * 获取 WiFi 信号强度
     */
    private fun getWifiSignalStrength(capabilities: NetworkCapabilities): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ 可以使用 signalStrength
                val signalStrength = capabilities.signalStrength
                if (signalStrength in 0..100) {
                    signalStrength
                } else {
                    50 // 默认值
                }
            } else {
                // API < 29，无法直接获取，返回默认值
                50
            }
        } catch (_: Exception) {
            50
        }
    }

    /**
     * 获取移动网络信号强度
     */
    private fun getCellularSignalStrength(capabilities: NetworkCapabilities): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signalStrength = capabilities.signalStrength
                if (signalStrength in 0..100) {
                    signalStrength
                } else {
                    50 // 默认值
                }
            } else {
                // API < 30，无法直接获取，返回默认值
                50
            }
        } catch (_: Exception) {
            50
        }
    }

    /**
     * 更新网络状态（内部方法）
     */
    private fun updateState(newState: NetworkState) {
        try {
            val oldState = _networkStateFlow.value
            if (oldState != newState) {
                _networkStateFlow.value = newState
                notifyListeners(oldState, newState)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 通知监听器（带异常保护）
     * 使用快照机制，避免并发修改问题
     */
    private fun notifyListeners(oldState: NetworkState, newState: NetworkState) {
        // 使用快照避免并发修改问题
        val snapshot = ArrayList(listeners)
        val toRemove = mutableListOf<NetworkStateListener>()

        snapshot.forEach { listener ->
            try {
                listener.onNetworkStateChanged(oldState, newState)
            } catch (_: Exception) {
                toRemove.add(listener)
            }
        }

        // 移除异常的监听器
        toRemove.forEach { listener ->
            try {
                listeners.remove(listener)
            } catch (_: Exception) {
            }
        }
    }
}
