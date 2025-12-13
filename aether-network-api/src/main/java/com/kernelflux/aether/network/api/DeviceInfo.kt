package com.kernelflux.aether.network.api

/**
 * 设备信息
 * 用于针对中低端机型进行优化
 */
data class DeviceInfo(
    /**
     * 设备型号
     */
    val model: String = "",
    
    /**
     * 制造商
     */
    val manufacturer: String = "",
    
    /**
     * Android 版本
     */
    val androidVersion: Int = 0,
    
    /**
     * 可用内存（MB）
     */
    val availableMemoryMB: Long = 0L,
    
    /**
     * 总内存（MB）
     */
    val totalMemoryMB: Long = 0L,
    
    /**
     * CPU 核心数
     */
    val cpuCores: Int = 0,
    
    /**
     * CPU 频率（MHz）
     */
    val cpuFrequencyMHz: Long = 0L,
    
    /**
     * 是否为低端设备
     */
    val isLowEndDevice: Boolean = false,
    
    /**
     * 是否为中端设备
     */
    val isMidRangeDevice: Boolean = false,
    
    /**
     * ROM 类型（用于多厂商适配）
     */
    val romType: RomType = RomType.UNKNOWN,
    
    /**
     * 是否在省电模式
     */
    val isPowerSaveMode: Boolean = false,
    
    /**
     * 是否在后台
     */
    val isBackground: Boolean = false
)

/**
 * ROM 类型
 * 用于适配国内多厂商个性化 ROM
 */
enum class RomType {
    /**
     * 未知/原生 Android
     */
    UNKNOWN,
    
    /**
     * MIUI（小米）
     */
    MIUI,
    
    /**
     * EMUI/HarmonyOS（华为）
     */
    EMUI,
    
    /**
     * ColorOS（OPPO）
     */
    COLOROS,
    
    /**
     * OriginOS（vivo）
     */
    ORIGINOS,
    
    /**
     * OneUI（三星）
     */
    ONEUI,
    
    /**
     * Flyme（魅族）
     */
    FLYME,
    
    /**
     * FuntouchOS（vivo 旧版）
     */
    FUNTOUCHOS,
    
    /**
     * Realme UI（realme）
     */
    REALMEUI,
    
    /**
     * OxygenOS（一加）
     */
    OXYGENOS,
    
    /**
     * 其他
     */
    OTHER
}

/**
 * 设备性能检测器
 * 用于检测设备性能，针对中低端机型进行优化
 */
interface DevicePerformanceDetector {
    /**
     * 检测设备信息
     * @return 设备信息
     */
    fun detectDeviceInfo(): DeviceInfo
    
    /**
     * 判断是否为低端设备
     * @param deviceInfo 设备信息
     * @return 是否为低端设备
     */
    fun isLowEndDevice(deviceInfo: DeviceInfo): Boolean
    
    /**
     * 判断是否为中端设备
     * @param deviceInfo 设备信息
     * @return 是否为中端设备
     */
    fun isMidRangeDevice(deviceInfo: DeviceInfo): Boolean
    
    /**
     * 获取设备性能评分（0-100）
     * @param deviceInfo 设备信息
     * @return 性能评分
     */
    fun getPerformanceScore(deviceInfo: DeviceInfo): Int
    
    /**
     * 检测 ROM 类型
     * @return ROM 类型
     */
    fun detectRomType(): RomType
    
    /**
     * 是否在省电模式
     * @return 是否在省电模式
     */
    fun isPowerSaveMode(): Boolean
    
    /**
     * 是否在后台
     * @return 是否在后台
     */
    fun isBackground(): Boolean
}

/**
 * 设备性能阈值
 */
data class DevicePerformanceThreshold(
    /**
     * 低端设备内存阈值（MB）
     */
    val lowEndMemoryThreshold: Long = 2048L, // 2GB
    
    /**
     * 中端设备内存阈值（MB）
     */
    val midRangeMemoryThreshold: Long = 4096L, // 4GB
    
    /**
     * 低端设备 CPU 核心数阈值
     */
    val lowEndCpuCoresThreshold: Int = 4,
    
    /**
     * 低端设备 CPU 频率阈值（MHz）
     */
    val lowEndCpuFreqThreshold: Long = 1500L // 1.5GHz
)
