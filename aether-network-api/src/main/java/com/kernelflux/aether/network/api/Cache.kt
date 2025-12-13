package com.kernelflux.aether.network.api

/**
 * 缓存策略
 */
enum class CacheStrategy {
    /**
     * 不使用缓存
     */
    NO_CACHE,
    
    /**
     * 仅使用缓存，如果缓存不存在则失败
     */
    CACHE_ONLY,
    
    /**
     * 优先使用缓存，缓存不存在则请求网络
     */
    CACHE_FIRST,
    
    /**
     * 优先请求网络，失败则使用缓存
     */
    NETWORK_FIRST,
    
    /**
     * 同时请求网络和缓存，优先返回缓存，网络返回后更新缓存
     */
    CACHE_AND_NETWORK,
    
    /**
     * 仅当缓存过期时才请求网络
     */
    CACHE_IF_AVAILABLE
}

/**
 * 缓存接口
 */
interface Cache {
    /**
     * 获取缓存
     */
    fun get(key: String): CacheEntry?
    
    /**
     * 存储缓存
     */
    fun put(key: String, entry: CacheEntry)
    
    /**
     * 删除缓存
     */
    fun remove(key: String)
    
    /**
     * 清空所有缓存
     */
    fun clear()
    
    /**
     * 获取缓存大小
     */
    fun size(): Long
}

/**
 * 缓存条目
 */
data class CacheEntry(
    val data: ByteArray,
    val headers: Map<String, List<String>>,
    val timestamp: Long,
    val ttl: Long = 0L, // 生存时间（毫秒），0 表示永久有效
    val etag: String? = null,
    val lastModified: String? = null
) {
    /**
     * 是否过期
     */
    fun isExpired(): Boolean {
        if (ttl <= 0) return false
        return System.currentTimeMillis() - timestamp > ttl
    }
    
    /**
     * 是否有效（未过期）
     */
    fun isValid(): Boolean = !isExpired()
}

/**
 * 缓存键生成器
 */
interface CacheKeyGenerator {
    /**
     * 生成缓存键
     */
    fun generate(request: Request): String
}
