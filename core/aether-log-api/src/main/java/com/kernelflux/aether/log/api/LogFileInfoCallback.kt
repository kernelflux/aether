package com.kernelflux.aether.log.api

/**
 * 日志文件信息查询回调接口
 * 用于 Java 代码异步获取日志文件信息
 * 
 * Kotlin 代码建议使用 suspend 函数版本的 getLogFileInfosAsync
 */
interface LogFileInfoCallback {
    /**
     * 查询成功
     * @param fileInfos 日志文件信息列表（按时间倒序，最新的在前）
     */
    fun onSuccess(fileInfos: List<LogFileInfo>)
    
    /**
     * 查询失败
     * @param error 错误信息
     */
    fun onError(error: Throwable)
}

