package com.kernelflux.aether.log.spi

/**
 * 日志级别
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * 日志服务接口
 * 
 * @author Aether Framework
 */
interface ILogger {
    
    /**
     * 设置日志级别
     */
    fun setLogLevel(level: LogLevel)
    
    /**
     * 是否启用日志
     */
    fun setEnabled(enabled: Boolean)
    
    /**
     * Verbose日志
     */
    fun v(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Debug日志
     */
    fun d(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Info日志
     */
    fun i(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Warn日志
     */
    fun w(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Error日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

