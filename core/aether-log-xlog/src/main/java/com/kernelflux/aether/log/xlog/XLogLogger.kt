package com.kernelflux.aether.log.xlog

import android.content.Context
import com.kernelflux.aether.log.api.AppenderMode
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LoggerHelper
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.fluxrouter.annotation.FluxService
import java.io.File

/**
 * XLog 日志服务实现
 * 
 * 通过 @FluxService 注册到 FluxRouter
 * 用户通过 FluxRouter.getService(ILogger::class.java) 获取
 * 
 * @author Aether Framework
 */
@FluxService(interfaceClass = ILogger::class)
class XLogLogger : ILogger {
    
    private var globalContext: Context? = null
    private var config: LoggerConfig = LoggerConfig()
    private var enabled: Boolean = true
    private var currentLevel: LogLevel = LogLevel.DEBUG
    private var isInitialized = false
    
    override fun init(context: Context, defaultConfig: LoggerConfig?) {
        synchronized(this) {
            if (isInitialized) return
            
            globalContext = context.applicationContext ?: context
            if (defaultConfig != null) {
                this.config = defaultConfig
                this.enabled = defaultConfig.enabled
                this.currentLevel = defaultConfig.level
            }
            
            // 初始化 xlog（即使 fileConfig 为 null，也使用默认配置初始化）
            try {
                initializeXlog()
                isInitialized = true
            } catch (e: Exception) {
                android.util.Log.e(
                    "XLogLogger",
                    "Failed to initialize xlog during init()",
                    e
                )
                // 即使初始化失败，也标记为已初始化，后续会使用 Android Log 作为后备
                isInitialized = true
            }
        }
    }
    
    override fun setLogLevel(level: LogLevel) {
        currentLevel = level
        config = config.copy(level = level)
        if (enabled && isInitialized) {
            Xlog.setLogLevel(mapLogLevel(level))
        }
    }
    
    override fun getLogLevel(): LogLevel? {
        return if (enabled && isInitialized) {
            val nativeLevel = Xlog.getLogLevelNative()
            mapNativeLogLevel(nativeLevel) ?: currentLevel
        } else {
            null
        }
    }
    
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        config = config.copy(enabled = enabled)
        if (isInitialized) {
            if (!enabled) {
                Xlog.setLogLevel(Xlog.LEVEL_NONE)
            } else {
                Xlog.setLogLevel(mapLogLevel(currentLevel))
            }
        }
    }
    
    override fun v(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.VERBOSE, enabled, currentLevel)) return
        if (!ensureInitialized()) {
            // 如果初始化失败，使用 Android Log 作为后备
            android.util.Log.v(tag, message, throwable)
            return
        }
        
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Xlog.log(Xlog.LEVEL_VERBOSE, formattedTag, logMessage)
    }
    
    override fun d(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.DEBUG, enabled, currentLevel)) return
        if (!ensureInitialized()) {
            // 如果初始化失败，使用 Android Log 作为后备
            android.util.Log.d(tag, message, throwable)
            return
        }
        
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Xlog.log(Xlog.LEVEL_DEBUG, formattedTag, logMessage)
    }
    
    override fun i(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.INFO, enabled, currentLevel)) return
        if (!ensureInitialized()) {
            // 如果初始化失败，使用 Android Log 作为后备
            android.util.Log.i(tag, message, throwable)
            return
        }
        
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Xlog.log(Xlog.LEVEL_INFO, formattedTag, logMessage)
    }
    
    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.WARN, enabled, currentLevel)) return
        if (!ensureInitialized()) {
            // 如果初始化失败，使用 Android Log 作为后备
            android.util.Log.w(tag, message, throwable)
            return
        }
        
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Xlog.log(Xlog.LEVEL_WARNING, formattedTag, logMessage)
    }
    
    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (!LoggerHelper.shouldLog(LogLevel.ERROR, enabled, currentLevel)) return
        if (!ensureInitialized()) {
            // 如果初始化失败，使用 Android Log 作为后备
            android.util.Log.e(tag, message, throwable)
            return
        }
        
        val logMessage = LoggerHelper.formatMessage(message, throwable)
        val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
        Xlog.log(Xlog.LEVEL_ERROR, formattedTag, logMessage)
    }
    
    override fun flush(isSync: Boolean) {
        if (enabled && isInitialized) {
            Xlog.appenderFlushNative(isSync)
        }
    }
    
    override fun close() {
        if (enabled && isInitialized) {
            Xlog.appenderCloseNative()
            isInitialized = false
        }
    }
    
    override fun setAppenderMode(mode: AppenderMode) {
        if (enabled && isInitialized) {
            val nativeMode = when (mode) {
                AppenderMode.ASYNC -> Xlog.APPENDER_MODE_ASYNC
                AppenderMode.SYNC -> Xlog.APPENDER_MODE_SYNC
            }
            Xlog.setAppenderMode(nativeMode)
        }
    }
    
    private fun ensureInitialized(): Boolean {
        if (isInitialized) return true
        
        return try {
            if (globalContext != null && config.fileConfig != null) {
                initializeXlog()
                true
            } else {
                // 如果 context 或 fileConfig 为 null，尝试使用默认配置初始化
                if (globalContext != null) {
                    // 使用默认配置初始化（不依赖 fileConfig）
                    val appContext = globalContext?.applicationContext ?: globalContext
                    if (appContext != null) {
                        val cacheDir = File(appContext.cacheDir, "xlog_cache").absolutePath
                        val logDir = File(appContext.filesDir, "xlog").absolutePath
                        File(cacheDir).mkdirs()
                        File(logDir).mkdirs()
                        
                        Xlog.open(
                            isLoadLib = true,
                            level = mapLogLevel(config.level),
                            mode = Xlog.APPENDER_MODE_ASYNC,
                            cacheDir = cacheDir,
                            logDir = logDir,
                            namePrefix = "aether",
                            cacheDays = 0,
                            pubkey = null,
                            isCompress = config.fileConfig?.compressEnabled ?: true
                        )
                        Xlog.setConsoleLogOpen(config.consoleEnabled)
                        isInitialized = true
                        true
                    } else {
                        android.util.Log.w("XLogLogger", "Context is null, cannot initialize xlog")
                        false
                    }
                } else {
                    android.util.Log.w("XLogLogger", "GlobalContext is null, cannot initialize xlog")
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "XLogLogger",
                "Failed to initialize xlog",
                e
            )
            false
        }
    }
    
    private fun initializeXlog() {
        if (isInitialized) return
        
        val appContext = globalContext?.applicationContext ?: globalContext
            ?: throw IllegalStateException("Context is null")
        
        // 使用配置中的文件配置，或使用默认值
        val fileConfig = config.fileConfig
        val cacheDir = fileConfig?.cacheDir
            ?: File(appContext.cacheDir, "xlog_cache").absolutePath
        val logDir = fileConfig?.logDir
            ?: File(appContext.filesDir, "xlog").absolutePath
        val namePrefix = fileConfig?.namePrefix ?: "aether"
        val cacheDays = fileConfig?.cacheDays ?: 0
        
        // Create directories
        File(cacheDir).mkdirs()
        File(logDir).mkdirs()
        
        // Convert custom header info Map to String format
        val customHeaderInfoString = fileConfig?.customHeaderInfo?.let { headerMap ->
            if (headerMap.isNotEmpty()) {
                headerMap.entries.joinToString(separator = "\n") { (key, value) ->
                    "$key: $value"
                }
            } else {
                null
            }
        }
        
        // Set custom header info BEFORE opening xlog (so it's included in the header)
        customHeaderInfoString?.let {
            // Load library first if needed
            try {
                System.loadLibrary("c++_shared")
            } catch (e: UnsatisfiedLinkError) {
                // c++_shared may already be loaded by another library
            }
            try {
                System.loadLibrary("aetherxlog")
                Xlog.setCustomHeaderInfo(it)
            } catch (e: UnsatisfiedLinkError) {
                // Library not loaded yet, will be loaded in Xlog.open()
            }
        }
        
        // Initialize xlog
        Xlog.open(
            isLoadLib = true,
            level = mapLogLevel(config.level),
            mode = Xlog.APPENDER_MODE_ASYNC,
            cacheDir = cacheDir,
            logDir = logDir,
            namePrefix = namePrefix,
            cacheDays = cacheDays,
            pubkey = fileConfig?.pubkey,
            isCompress = fileConfig?.compressEnabled ?: true
        )
        
        // Set custom header info again after library is loaded (in case it wasn't set before)
        customHeaderInfoString?.let {
            Xlog.setCustomHeaderInfo(it)
        }
        
        // Enable console log based on config
        Xlog.setConsoleLogOpen(config.consoleEnabled)
        
        // Set file size and alive time if configured
        fileConfig?.let {
            Xlog.setMaxFileSize(it.maxFileSize)
            Xlog.setMaxAliveTime(it.maxAliveTime)
        }
        
        isInitialized = true
    }
    
    private fun mapLogLevel(level: LogLevel): Int {
        return when (level) {
            LogLevel.VERBOSE -> Xlog.LEVEL_VERBOSE
            LogLevel.DEBUG -> Xlog.LEVEL_DEBUG
            LogLevel.INFO -> Xlog.LEVEL_INFO
            LogLevel.WARN -> Xlog.LEVEL_WARNING
            LogLevel.ERROR -> Xlog.LEVEL_ERROR
        }
    }
    
    private fun mapNativeLogLevel(nativeLevel: Int): LogLevel? {
        return when (nativeLevel) {
            Xlog.LEVEL_VERBOSE -> LogLevel.VERBOSE
            Xlog.LEVEL_DEBUG -> LogLevel.DEBUG
            Xlog.LEVEL_INFO -> LogLevel.INFO
            Xlog.LEVEL_WARNING -> LogLevel.WARN
            Xlog.LEVEL_ERROR -> LogLevel.ERROR
            Xlog.LEVEL_NONE -> null // LEVEL_NONE means disabled
            else -> null
        }
    }
}
