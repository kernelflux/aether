package com.kernelflux.aether.log.api

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
 * Appender 模式
 */
enum class AppenderMode {
    /**
     * 异步模式：日志写入缓冲区，由后台线程异步写入文件
     * 性能更好，但崩溃时可能丢失部分日志
     */
    ASYNC,
    
    /**
     * 同步模式：日志立即写入文件
     * 性能稍差，但能确保日志不丢失
     */
    SYNC
}

/**
 * 日志服务接口
 * 
 * 通过 @FluxService 注册到 FluxRouter，用户通过 FluxRouter.getService(ILogger::class.java) 获取
 * 
 * 使用方式：
 * ```kotlin
 * val logger = FluxRouter.getService(ILogger::class.java)
 * logger?.init(context, defaultConfig)
 * logger?.d("Tag", "Message")
 * ```
 * 
 * @author Aether Framework
 */
interface ILogger {
    
    /**
     * 初始化日志系统
     * 推荐在 Application.onCreate() 中调用
     * 
     * @param context Android Context
     * @param defaultConfig 全局默认配置（可选）
     */
    fun init(context: android.content.Context, defaultConfig: LoggerConfig? = null)
    
    /**
     * 设置日志级别
     */
    fun setLogLevel(level: LogLevel)
    
    /**
     * 获取当前日志级别
     * @return 当前日志级别，如果未初始化则返回 null
     */
    fun getLogLevel(): LogLevel?
    
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
    
    /**
     * 刷新日志缓冲区，确保日志写入磁盘
     * @param isSync 是否同步刷新（true：同步，确保立即写入；false：异步，通知后台线程刷新）
     */
    fun flush(isSync: Boolean = false)
    
    /**
     * 关闭日志系统，释放资源
     * 应该在应用退出时调用
     */
    fun close()
    
    /**
     * 设置 Appender 模式
     * @param mode Appender 模式（ASYNC：异步，性能好但可能丢日志；SYNC：同步，确保不丢日志）
     */
    fun setAppenderMode(mode: AppenderMode)
}

