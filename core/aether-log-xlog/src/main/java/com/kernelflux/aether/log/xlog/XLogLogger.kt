package com.kernelflux.aether.log.xlog

import android.content.Context
import com.kernelflux.aether.log.api.AppenderMode
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LibraryLoader
import com.kernelflux.aether.log.api.LogFileInfo
import com.kernelflux.aether.log.api.LogFileInfoCallback
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LoggerHelper
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.fluxrouter.annotation.FluxService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * XLog 日志服务实现
 *
 * 通过 @FluxService 注册到 FluxRouter
 * 用户通过 FluxRouter.getService(ILogger::class.java) 获取
 */
@FluxService(interfaceClass = ILogger::class)
class XLogLogger : ILogger {

    private var globalContext: Context? = null
    private var config: LoggerConfig = LoggerConfig()
    private var enabled: Boolean = true
    private var currentLevel: LogLevel = LogLevel.DEBUG
    private var isInitialized = false
    private val instanceMap = mutableMapOf<String, Long>()
    private val instanceMapLock = Any()
    private val moduleNameThreadLocal = ThreadLocal<String?>()
    private val defaultInstancePtr: Long = 0L

    override fun init(context: Context, defaultConfig: LoggerConfig?) {
        synchronized(this) {
            if (isInitialized) return

            globalContext = context.applicationContext ?: context
            if (defaultConfig != null) {
                this.config = defaultConfig
                this.enabled = defaultConfig.enabled
                this.currentLevel = defaultConfig.level
                
                // 设置自定义 so 库加载器（如果配置中提供了）
                // 必须在 initializeXlog() 之前设置，否则无法生效
                defaultConfig.libraryLoader?.let { loader ->
                    Xlog.setLibraryLoader(loader)
                }
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

    override fun withModule(moduleName: String): ILogger {
        // 设置当前线程的模块名
        moduleNameThreadLocal.set(moduleName)
        return this
    }
    
    /**
     * 获取或创建模块实例
     */
    private fun getOrCreateInstance(moduleName: String): Long {
        synchronized(instanceMapLock) {
            // 先尝试获取已存在的实例
            instanceMap[moduleName]?.let { instancePtr ->
                if (instancePtr != 0L) {
                    // 验证实例是否仍然有效
                    val existingInstance = Xlog.getXlogInstance(moduleName)
                    if (existingInstance != 0L) {
                        return existingInstance
                    } else {
                        // 实例已被释放，从 map 中移除
                        instanceMap.remove(moduleName)
                    }
                }
            }
            
            // 创建新实例
            val appContext = globalContext?.applicationContext ?: globalContext
                ?: return defaultInstancePtr
            
            val fileConfig = config.fileConfig
            val cacheDir = fileConfig?.cacheDir
                ?: File(appContext.cacheDir, "xlog_cache").absolutePath
            val logDir = fileConfig?.logDir
                ?: File(appContext.filesDir, "xlog").absolutePath
            val cacheDays = fileConfig?.cacheDays ?: 0
            
            // 确保目录存在
            File(cacheDir).mkdirs()
            File(logDir).mkdirs()
            
            val instancePtr = Xlog.newXlogInstance(
                level = mapLogLevel(config.level),
                mode = Xlog.APPENDER_MODE_ASYNC,
                cacheDir = cacheDir,
                logDir = logDir,
                namePrefix = moduleName,
                cacheDays = cacheDays,
                pubkey = fileConfig?.pubkey,
                isCompress = fileConfig?.compressEnabled ?: true
            )
            
            if (instancePtr != 0L) {
                instanceMap[moduleName] = instancePtr
            }
            
            return instancePtr
        }
    }
    
    /**
     * 获取当前使用的实例指针
     */
    private fun getCurrentInstancePtr(): Long {
        val moduleName = moduleNameThreadLocal.get()
        return if (moduleName != null) {
            getOrCreateInstance(moduleName)
        } else {
            defaultInstancePtr
        }
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
    private fun logInternal(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        try {
            if (!LoggerHelper.shouldLog(level, enabled, currentLevel)) return
            if (!ensureInitialized()) {
                // 如果初始化失败，使用 Android Log 作为后备
                try {
                    when (level) {
                        LogLevel.VERBOSE -> android.util.Log.v(tag, message, throwable)
                        LogLevel.DEBUG -> android.util.Log.d(tag, message, throwable)
                        LogLevel.INFO -> android.util.Log.i(tag, message, throwable)
                        LogLevel.WARN -> android.util.Log.w(tag, message, throwable)
                        LogLevel.ERROR -> android.util.Log.e(tag, message, throwable)
                    }
                } catch (e: Exception) {
                    // 防止非法字符导致crash，使用安全的日志打印
                    LoggerHelper.safeLogError(
                        "XLogLogger",
                        "Failed to log ${level.name.lowercase()} message",
                        e
                    )
                }
                return
            }

            val logMessage = LoggerHelper.formatMessage(message, throwable)
            val formattedTag = LoggerHelper.formatTag(tag, config.tagPrefix)
            val nativeLevel = mapLogLevel(level)
            val instancePtr = getCurrentInstancePtr()
            Xlog.log(instancePtr, nativeLevel, formattedTag, logMessage)
        } catch (e: Exception) {
            // 防止任何异常导致crash，使用安全的日志打印
            LoggerHelper.safeLogError(
                "XLogLogger",
                "Failed to log ${level.name.lowercase()} message",
                e
            )
        } finally {
            // 清除 ThreadLocal，避免影响后续不带模块名的日志
            moduleNameThreadLocal.remove()
        }
    }

    override fun flush(isSync: Boolean) {
        if (enabled && isInitialized) {
            // 手动刷新：刷新所有实例（包括默认实例和所有模块实例）
            // 注意：这是用户主动调用的，会强制刷新缓冲区数据到文件
            // 与自动刷新不同（自动刷新只在缓冲区达到 1/3 或 FATAL 时触发）
            Xlog.flushAll(isSync)
        }
    }

    override fun flushModule(moduleName: String, isSync: Boolean): Boolean {
        if (!enabled || !isInitialized) {
            return false
        }
        
        // 检查模块是否存在
        val moduleExists = synchronized(instanceMapLock) {
            instanceMap.containsKey(moduleName)
        }
        
        if (!moduleExists) {
            return false
        }
        
        // 刷新模块日志
        // 注意：不会造成重复落盘，因为：
        // 1. FlushSync() 会检查缓冲区是否为空，如果为空则直接返回
        // 2. LogBuffer::Flush() 会调用 __Clear() 清空缓冲区，确保不会重复写入
        // 3. 使用 mutex_buffer_async_ 锁保护，避免异步线程和同步刷新同时进行
        Xlog.flushModule(moduleName, isSync)

        return true
    }

    override fun getLogFileInfos(moduleName: String): List<LogFileInfo> {
        if (!enabled || !isInitialized) {
            return emptyList()
        }
        
        // 检查模块是否存在
        synchronized(instanceMapLock) {
            if (!instanceMap.containsKey(moduleName)) {
                return emptyList()
            }
        }
        
        // 获取日志文件信息
        val infos = Xlog.getLogFileInfos(moduleName)
        return infos.toList()
    }

    override fun getLogFileInfos(moduleName: String, daysAgo: Int): List<LogFileInfo> {
        if (!enabled || !isInitialized) {
            return emptyList()
        }
        
        // 检查模块是否存在
        synchronized(instanceMapLock) {
            if (!instanceMap.containsKey(moduleName)) {
                return emptyList()
            }
        }
        
        // 获取指定天数前的日志文件信息
        val infos = Xlog.getLogFileInfosByDays(moduleName, daysAgo)
        return infos.toList()
    }

    override fun getLogFileInfos(moduleName: String, startTime: Long, endTime: Long): List<LogFileInfo> {
        if (!enabled || !isInitialized) {
            return emptyList()
        }
        
        // 检查模块是否存在
        synchronized(instanceMapLock) {
            if (!instanceMap.containsKey(moduleName)) {
                return emptyList()
            }
        }
        
        // 获取时间范围内的日志文件信息
        val infos = Xlog.getLogFileInfosByTimeRange(moduleName, startTime, endTime)
        return infos.toList()
    }

    override fun flushModules(moduleNames: List<String>, isSync: Boolean): List<String> {
        if (!enabled || !isInitialized) {
            return emptyList()
        }
        
        val successModules = mutableListOf<String>()
        
        moduleNames.forEach { moduleName ->
            if (flushModule(moduleName, isSync)) {
                successModules.add(moduleName)
            }
        }
        
        return successModules
    }

    override fun getAllModules(): List<String> {
        if (!enabled || !isInitialized) {
            return emptyList()
        }
        
        synchronized(instanceMapLock) {
            return instanceMap.keys.toList()
        }
    }

    override suspend fun getLogFileInfosAsync(
        moduleName: String,
        startTime: Long,
        endTime: Long
    ): List<LogFileInfo> {
        // 不创建新的协程作用域，完全依赖调用方的协程作用域
        // 只做必要的线程切换（IO 操作），让调用方完全控制协程生命周期
        return withContext(Dispatchers.IO) {
            if (!enabled || !isInitialized) {
                return@withContext emptyList()
            }
            
            // 检查模块是否存在
            val moduleExists = synchronized(instanceMapLock) {
                instanceMap.containsKey(moduleName)
            }
            if (!moduleExists) {
                return@withContext emptyList()
            }
            getLogFileInfos(moduleName, startTime, endTime)
        }
    }
    
    override fun getLogFileInfosAsync(
        moduleName: String,
        startTime: Long,
        endTime: Long,
        callback: LogFileInfoCallback,
        executor: java.util.concurrent.Executor?
    ) {
        if(executor==null){
            callback.onError(IllegalArgumentException("Executor cannot be null. Use the overload without executor parameter for default behavior."))
            return
        }
        executor.execute {
            try {
                val result = if (!enabled || !isInitialized) {
                    emptyList()
                } else {
                    val moduleExists = synchronized(instanceMapLock) {
                        instanceMap.containsKey(moduleName)
                    }
                    if (moduleExists) {
                        getLogFileInfos(moduleName, startTime, endTime)
                    } else {
                        emptyList()
                    }
                }
                callback.onSuccess(result)
            } catch (e: Throwable) {
                callback.onError(e)
            }
        }
    }
    
    override fun getLogFileInfosAsync(
        moduleName: String,
        startTime: Long,
        endTime: Long,
        callback: LogFileInfoCallback
    ) {
        val defaultScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        defaultScope.launch(Dispatchers.IO) {
            try {
                val result = getLogFileInfosAsync(moduleName, startTime, endTime)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    override fun clearFileCache(moduleName: String) {
        if (!enabled || !isInitialized) {
            return
        }
        
        // 检查模块是否存在
        synchronized(instanceMapLock) {
            if (instanceMap.containsKey(moduleName)) {
                Xlog.clearFileCache(moduleName)
            }
        }
    }

    override fun clearAllFileCache() {
        if (!enabled || !isInitialized) {
            return
        }
        
        Xlog.clearAllFileCache()
    }
    

    override fun close() {
        if (enabled && isInitialized) {
            // 关闭所有模块实例
            synchronized(instanceMapLock) {
                instanceMap.forEach { (moduleName, _) ->
                    Xlog.releaseXlogInstance(moduleName)
                }
                instanceMap.clear()
            }
            // 关闭默认实例
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
            if (globalContext == null) {
                android.util.Log.w(
                    "XLogLogger",
                    "GlobalContext is null, cannot initialize xlog"
                )
                return false
            }
            initializeXlog()
            true
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

        try {
            Xlog.loadLibraryInternal("c++_shared")
        } catch (e: UnsatisfiedLinkError) {
            // c++_shared may already be loaded by another library
        }
        
        // Always load aetherxlog library
        Xlog.loadLibraryInternal("aetherxlog")
        
        // Set custom header info BEFORE opening xlog (so it's included in the header)
        customHeaderInfoString?.let {
            Xlog.setCustomHeaderInfo(it)
        }

        // Initialize xlog (libraries are already loaded, so isLoadLib = false)
        Xlog.open(
            level = mapLogLevel(config.level),
            mode = Xlog.APPENDER_MODE_ASYNC,
            cacheDir = cacheDir,
            logDir = logDir,
            namePrefix = namePrefix,
            cacheDays = cacheDays,
            pubkey = fileConfig?.pubkey,
            isCompress = fileConfig?.compressEnabled ?: true
        )

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
