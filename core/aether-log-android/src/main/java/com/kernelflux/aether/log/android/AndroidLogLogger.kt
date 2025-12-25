package com.kernelflux.aether.log.android

import android.content.Context
import android.util.Log
import com.kernelflux.aether.log.api.AppenderMode
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LogFileInfo
import com.kernelflux.aether.log.api.LogFileInfoCallback
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LoggerHelper
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.fluxrouter.annotation.FluxService

/**
 * Android Log 日志服务实现
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
    
    override fun withModule(moduleName: String): ILogger {
        // 注意：Android Log 不支持文件输出，所以模块名在这里不起作用
        return this
    }
    
    override fun v(tag: String, message: String, throwable: Throwable?) {
        logInternal(LogLevel.VERBOSE, tag, message, throwable)
    }
    
    override fun d(tag: String, message: String, throwable: Throwable?) {
        logInternal(LogLevel.DEBUG, tag, message, throwable)
    }
    
    override fun i(tag: String, message: String, throwable: Throwable?) {
        logInternal(LogLevel.INFO, tag, message, throwable)
    }
    
    override fun w(tag: String, message: String, throwable: Throwable?) {
        logInternal(LogLevel.WARN, tag, message, throwable)
    }
    
    override fun e(tag: String, message: String, throwable: Throwable?) {
        logInternal(LogLevel.ERROR, tag, message, throwable)
    }
    
    /**
     * 内部日志方法
     */
    private fun logInternal(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        try {
            if (!LoggerHelper.shouldLog(level, enabled, currentLevel)) return
            val logMessage = LoggerHelper.formatMessage(message, throwable)
            val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
            when (level) {
                LogLevel.VERBOSE -> Log.v(formattedTag, logMessage)
                LogLevel.DEBUG -> Log.d(formattedTag, logMessage)
                LogLevel.INFO -> Log.i(formattedTag, logMessage)
                LogLevel.WARN -> Log.w(formattedTag, logMessage)
                LogLevel.ERROR -> Log.e(formattedTag, logMessage)
            }
        } catch (e: Exception) {
            // 防止非法字符导致crash，使用安全的日志打印
            LoggerHelper.safeLogError("AndroidLogLogger", "Failed to log ${level.name.lowercase()} message", e)
        }
    }
    
    override fun flush(isSync: Boolean) {
        // Android Log 不需要刷新
        Log.d("AndroidLogLogger", "flush called (no-op for Android Log)")
    }

    override fun flushModule(moduleName: String, isSync: Boolean): Boolean {
        return false
    }

    override fun getLogFileInfos(moduleName: String): List<LogFileInfo> {
        return emptyList()
    }

    override fun getLogFileInfos(
        moduleName: String,
        daysAgo: Int
    ): List<LogFileInfo> {
        return emptyList()
    }

    override fun getLogFileInfos(
        moduleName: String,
        startTime: Long,
        endTime: Long
    ): List<LogFileInfo> {
        return emptyList()
    }

    override suspend fun getLogFileInfosAsync(
        moduleName: String,
        startTime: Long,
        endTime: Long
    ): List<LogFileInfo> {
        // Android Log 不支持文件输出，返回空列表
        return emptyList()
    }
    
    override fun getLogFileInfosAsync(
        moduleName: String,
        startTime: Long,
        endTime: Long,
        callback: LogFileInfoCallback,
        executor: java.util.concurrent.Executor?
    ) {
        // Android Log 不支持文件输出，直接回调空列表
        if (executor != null) {
            executor.execute {
                callback.onSuccess(emptyList())
            }
        } else {
            // 如果没有提供 executor，在主线程回调
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback.onSuccess(emptyList())
            }
        }
    }
    
    override fun getLogFileInfosAsync(
        moduleName: String,
        startTime: Long,
        endTime: Long,
        callback: LogFileInfoCallback
    ) {
        // Android Log 不支持文件输出，直接回调空列表
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            callback.onSuccess(emptyList())
        }
    }

    override fun flushModules(
        moduleNames: List<String>,
        isSync: Boolean
    ): List<String> {
        return emptyList()
    }

    override fun getAllModules(): List<String> {
        return emptyList()
    }

    override fun clearFileCache(moduleName: String) {
    }

    override fun clearAllFileCache() {

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
