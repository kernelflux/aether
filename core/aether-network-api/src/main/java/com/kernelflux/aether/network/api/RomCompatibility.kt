package com.kernelflux.aether.network.api

/**
 * ROM 兼容性处理器
 */
interface RomCompatibilityHandler {
    /**
     * 检测 ROM 类型
     * @return ROM 类型
     */
    fun detectRomType(): RomType
    
    /**
     * 检查后台限制
     * @return 是否有限制
     */
    fun checkBackgroundRestriction(): Boolean
    
    /**
     * 检查网络权限
     * @return 是否有权限
     */
    fun checkNetworkPermission(): Boolean
    
    /**
     * 检查省电模式限制
     * @return 是否有限制
     */
    fun checkPowerSaveRestriction(): Boolean
    
    /**
     * 获取 ROM 特定的网络配置建议
     * @param romType ROM 类型
     * @return 配置建议
     */
    fun getRomSpecificConfig(romType: RomType): RomNetworkConfig
    
    /**
     * 应用 ROM 特定的优化
     * @param config 网络配置
     * @param romType ROM 类型
     * @return 优化后的配置
     */
    fun applyRomOptimization(
        config: NetworkConfig,
        romType: RomType
    ): NetworkConfig
    
    /**
     * 处理后台网络请求限制
     * @param request 请求
     * @param isBackground 是否在后台
     * @return 处理后的请求（可能需要调整优先级或延迟）
     */
    fun handleBackgroundRestriction(
        request: Request,
        isBackground: Boolean
    ): Request
    
    /**
     * 检查是否需要应用保活
     * @param romType ROM 类型
     * @return 是否需要保活
     */
    fun needKeepAlive(romType: RomType): Boolean
}

/**
 * ROM 特定的网络配置
 */
data class RomNetworkConfig(
    /**
     * 建议的连接超时时间（毫秒）
     */
    val suggestedConnectTimeout: Long? = null,
    
    /**
     * 建议的读取超时时间（毫秒）
     */
    val suggestedReadTimeout: Long? = null,
    
    /**
     * 是否建议使用 HTTP/2
     */
    val suggestHttp2: Boolean = true,
    
    /**
     * 是否建议启用连接池
     */
    val suggestConnectionPool: Boolean = true,
    
    /**
     * 连接池大小建议
     */
    val suggestedConnectionPoolSize: Int? = null,
    
    /**
     * 是否建议启用 DNS 缓存
     */
    val suggestDnsCache: Boolean = true,
    
    /**
     * DNS 缓存 TTL 建议（秒）
     */
    val suggestedDnsCacheTtl: Long? = null,
    
    /**
     * 是否建议启用请求压缩
     */
    val suggestRequestCompression: Boolean = true,
    
    /**
     * 是否建议启用响应压缩
     */
    val suggestResponseCompression: Boolean = true,
    
    /**
     * 后台请求优先级建议
     */
    val backgroundRequestPriority: RequestPriority = RequestPriority.LOW,
    
    /**
     * 是否需要特殊处理
     */
    val needSpecialHandling: Boolean = false,
    
    /**
     * 特殊处理说明
     */
    val specialHandlingNote: String? = null
)
