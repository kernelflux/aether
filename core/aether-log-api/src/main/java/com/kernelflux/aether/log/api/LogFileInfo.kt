package com.kernelflux.aether.log.api

import java.io.File

/**
 * 日志文件信息
 * 用于日志上报时提供文件的详细信息
 */
data class LogFileInfo(
    /**
     * 文件路径
     */
    val path: String,
    
    /**
     * 文件大小（字节）
     */
    val size: Long,
    
    /**
     * 文件修改时间（毫秒时间戳）
     */
    val lastModified: Long,
    
    /**
     * 模块名
     */
    val moduleName: String,
    
    /**
     * 文件类型（logDir 或 cacheDir）
     */
    val fileType: FileType
) {
    /**
     * 文件类型
     */
    enum class FileType {
        /** 正式日志目录 */
        LOG_DIR,
        /** 缓存目录 */
        CACHE_DIR
    }
    
    /**
     * 获取 File 对象
     */
    fun toFile(): File = File(path)
    
    /**
     * 文件是否存在
     */
    fun exists(): Boolean = File(path).exists()
}

