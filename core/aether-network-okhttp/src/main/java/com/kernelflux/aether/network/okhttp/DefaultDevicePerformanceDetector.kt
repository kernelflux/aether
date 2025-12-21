package com.kernelflux.aether.network.okhttp

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.kernelflux.aether.network.api.*

/**
 * 默认设备性能检测器实现
 */
class DefaultDevicePerformanceDetector(
    private val context: Context,
    private val threshold: DevicePerformanceThreshold = DevicePerformanceThreshold()
) : DevicePerformanceDetector {
    
    override fun detectDeviceInfo(): DeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        
        val totalMemoryMB = (memoryInfo.totalMem / (1024 * 1024))
        val availableMemoryMB = (memoryInfo.availMem / (1024 * 1024))
        
        // 检测 CPU 核心数
        val cpuCores = Runtime.getRuntime().availableProcessors()
        
        // 检测 ROM 类型
        val romType = detectRomType()
        
        // 判断设备等级
        val isLowEnd = isLowEndDevice(
            DeviceInfo(
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                androidVersion = Build.VERSION.SDK_INT,
                availableMemoryMB = availableMemoryMB,
                totalMemoryMB = totalMemoryMB,
                cpuCores = cpuCores,
                cpuFrequencyMHz = 0L, // 简化处理
                isLowEndDevice = false,
                isMidRangeDevice = false,
                romType = romType
            )
        )
        
        val isMidRange = isMidRangeDevice(
            DeviceInfo(
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                androidVersion = Build.VERSION.SDK_INT,
                availableMemoryMB = availableMemoryMB,
                totalMemoryMB = totalMemoryMB,
                cpuCores = cpuCores,
                cpuFrequencyMHz = 0L,
                isLowEndDevice = isLowEnd,
                isMidRangeDevice = false,
                romType = romType
            )
        )
        
        return DeviceInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.SDK_INT,
            availableMemoryMB = availableMemoryMB,
            totalMemoryMB = totalMemoryMB,
            cpuCores = cpuCores,
            cpuFrequencyMHz = 0L,
            isLowEndDevice = isLowEnd,
            isMidRangeDevice = isMidRange,
            romType = romType,
            isPowerSaveMode = isPowerSaveMode(),
            isBackground = isBackground()
        )
    }
    
    override fun isLowEndDevice(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.totalMemoryMB < threshold.lowEndMemoryThreshold ||
               deviceInfo.cpuCores < threshold.lowEndCpuCoresThreshold ||
               deviceInfo.cpuFrequencyMHz < threshold.lowEndCpuFreqThreshold
    }
    
    override fun isMidRangeDevice(deviceInfo: DeviceInfo): Boolean {
        return !isLowEndDevice(deviceInfo) &&
               deviceInfo.totalMemoryMB < threshold.midRangeMemoryThreshold
    }
    
    override fun getPerformanceScore(deviceInfo: DeviceInfo): Int {
        var score = 100
        
        // 内存影响
        when {
            deviceInfo.totalMemoryMB < 2048 -> score -= 30 // < 2GB
            deviceInfo.totalMemoryMB < 4096 -> score -= 15 // < 4GB
        }
        
        // CPU 核心数影响
        when {
            deviceInfo.cpuCores < 4 -> score -= 20
            deviceInfo.cpuCores < 6 -> score -= 10
        }
        
        // ROM 类型影响（某些 ROM 可能影响性能）
        when (deviceInfo.romType) {
            RomType.MIUI, RomType.EMUI -> score -= 5 // 可能有一些性能影响
            else -> {}
        }
        
        return score.coerceIn(0, 100)
    }
    
    override fun detectRomType(): RomType {
        val buildDisplay = Build.DISPLAY.uppercase()
        val buildBrand = Build.BRAND.uppercase()
        val buildManufacturer = Build.MANUFACTURER.uppercase()
        
        return when {
            buildDisplay.contains("MIUI") || buildBrand.contains("XIAOMI") -> RomType.MIUI
            buildDisplay.contains("EMUI") || buildDisplay.contains("HARMONY") || 
            buildBrand.contains("HUAWEI") || buildBrand.contains("HONOR") -> RomType.EMUI
            buildDisplay.contains("COLOROS") || buildBrand.contains("OPPO") -> RomType.COLOROS
            buildDisplay.contains("ORIGINOS") || buildDisplay.contains("FUNTOUCH") || 
            buildBrand.contains("VIVO") -> {
                if (buildDisplay.contains("ORIGINOS")) RomType.ORIGINOS
                else RomType.FUNTOUCHOS
            }
            buildDisplay.contains("ONEUI") || buildBrand.contains("SAMSUNG") -> RomType.ONEUI
            buildDisplay.contains("FLYME") || buildBrand.contains("MEIZU") -> RomType.FLYME
            buildDisplay.contains("REALME") || buildBrand.contains("REALME") -> RomType.REALMEUI
            buildDisplay.contains("OXYGEN") || buildBrand.contains("ONEPLUS") -> RomType.OXYGENOS
            else -> RomType.UNKNOWN
        }
    }
    
    override fun isPowerSaveMode(): Boolean {
        // 简化实现：无法准确检测，返回 false
        return false
    }
    
    override fun isBackground(): Boolean {
        // 简化实现：无法准确检测，返回 false
        return false
    }
}
