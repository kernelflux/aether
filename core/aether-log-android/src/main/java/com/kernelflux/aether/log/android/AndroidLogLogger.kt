package com.kernelflux.aether.log.android

import android.content.Context
import android.util.Log
import com.kernelflux.aether.log.api.AppenderMode
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LoggerHelper
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.fluxrouter.annotation.FluxService

/**
 * Android Log 日志服务实现
 * 
 * 通过 @FluxService 注册到 FluxRouter
 * 用户通过 FluxRouter.getService(ILogger::class.java) 获取
 * 
 * 使用 android.util.Log 作为日志输出
 * 
 * 特性：
 * - 轻量级，无需额外依赖
 * - 不支持文件输出
 * - 不支持加密
 * 
 * @author Aether Framework
 */
@FluxService(interfaceClass = ILogger::class)
class AndroidLogLogger : ILogger {
    
    private var config: LoggerConfig = LoggerConfig()
    private var enabled: Boolean = true
    private var currentLevel: LogLevel = LogLevel.DEBUG
    private var isInitialized = false
    
    override fun init(context: Context, defaultConfig: LoggerConfig?) {
        synchronized(this) {
            if (isInitialized) return
            
            if (defaultConfig != null) {
                this.config = defaultConfig
                this.enabled = defaultConfig.enabled
                this.currentLevel = defaultConfig.level
            }
            
            isInitialized = true
        }
    }
    
    override fun setLogLevel(level: LogLevel) {
        currentLevel = level
        config = config.copy(level = level)
        // Android Log 不控制系统日志级别，这里只更新内部状态
        Log.d("AndroidLogLogger", "Set log level to $level")
    }
    
    override fun getLogLevel(): LogLevel? {
        return if (enabled) {
            currentLevel
        } else {
            null
        }
    }
    
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        config = config.copy(enabled = enabled)
        Log.d("AndroidLogLogger", "Set enabled to $enabled")
    }
    
    override fun v(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.VERBOSE, enabled, currentLevel)) return
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Log.v(formattedTag, logMessage)
    }
    
    override fun d(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.DEBUG, enabled, currentLevel)) return
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Log.d(formattedTag, logMessage)
    }
    
    override fun i(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.INFO, enabled, currentLevel)) return
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Log.i(formattedTag, logMessage)
    }
    
    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.WARN, enabled, currentLevel)) return
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Log.w(formattedTag, logMessage)
    }
    
    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.ERROR, enabled, currentLevel)) return
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Log.e(formattedTag, logMessage)
    }
    
    override fun flush(isSync: Boolean) {
        // Android Log 不需要刷新
        Log.d("AndroidLogLogger", "flush called (no-op for Android Log)")
    }
    
    override fun close() {
        // Android Log 不需要关闭
        isInitialized = false
        Log.d("AndroidLogLogger", "close called (no-op for Android Log)")
    }
    
    override fun setAppenderMode(mode: AppenderMode) {
        // Android Log 不支持 Appender 模式
        Log.d("AndroidLogLogger", "setAppenderMode called (no-op for Android Log)")
    }
}
