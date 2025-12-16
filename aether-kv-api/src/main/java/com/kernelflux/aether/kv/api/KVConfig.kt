package com.kernelflux.aether.kv.api

import java.io.File

/**
 * KV 存储配置
 * 
 * @author Aether Framework
 */
data class KVConfig(
    /**
     * 存储根目录
     * 如果为 null，将使用应用数据目录
     */
    val rootDir: File? = null,
    
    /**
     * 存储文件名（用于区分不同的存储实例）
     * 默认为 "default"
     */
    val fileName: String = "default",
    
    /**
     * 是否启用加密（某些实现可能支持）
     * 默认为 false
     */
    val enableEncryption: Boolean = false,
    
    /**
     * 加密密钥（如果启用加密）
     */
    val encryptionKey: String? = null,
    
    /**
     * 最大存储大小（字节）
     * 默认为 0（无限制）
     */
    val maxSize: Long = 0L
) {
    /**
     * 配置构建器
     */
    class Builder {
        private var rootDir: File? = null
        private var fileName: String = "default"
        private var enableEncryption: Boolean = false
        private var encryptionKey: String? = null
        private var maxSize: Long = 0L
        
        fun rootDir(dir: File?) = apply { this.rootDir = dir }
        fun fileName(name: String) = apply { this.fileName = name }
        fun enableEncryption(enabled: Boolean) = apply { this.enableEncryption = enabled }
        fun encryptionKey(key: String?) = apply { this.encryptionKey = key }
        fun maxSize(size: Long) = apply { this.maxSize = size }
        
        fun build() = KVConfig(
            rootDir = rootDir,
            fileName = fileName,
            enableEncryption = enableEncryption,
            encryptionKey = encryptionKey,
            maxSize = maxSize
        )
    }
    
    companion object {
        /**
         * 创建默认配置
         */
        fun default() = KVConfig()
        
        /**
         * 创建配置构建器
         */
        fun builder() = Builder()
    }
}
