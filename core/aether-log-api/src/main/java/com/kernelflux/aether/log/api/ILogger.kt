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
     * 设置模块名，返回支持链式调用的日志记录器
     * 如果没有指定模块名，使用默认的模块名作为日志文件名
     * 
     * @param moduleName 模块名，如"network"、"payment"、"account"等
     * @return 支持链式调用的日志记录器（返回自身，支持链式调用）
     */
    fun withModule(moduleName: String): ILogger
    
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
     * 刷新指定模块的日志缓冲区，确保日志写入磁盘
     * 用于业务场景：少量日志不会自动落盘时，可以主动刷新特定模块的日志
     * 
     * 使用场景：
     * - 上报特定模块的日志前，先刷新确保日志已落盘
     * - 关键操作后，立即刷新相关模块的日志
     * 
     * @param moduleName 模块名，如 "account"、"payment"、"network" 等
     * @param isSync 是否同步刷新（true：同步，确保立即写入；false：异步，通知后台线程刷新）
     * @return 是否成功刷新（模块不存在时返回 false）
     */
    fun flushModule(moduleName: String, isSync: Boolean = true): Boolean

    /**
     * 获取指定模块的日志文件信息列表
     * 用于业务场景：上报日志时，需要获取日志文件的详细信息（路径、大小、时间等）
     * 
     * 返回当前日期及缓存目录中的日志文件（如果存在）
     * 
     * @param moduleName 模块名，如 "account"、"payment"、"network" 等
     * @return 日志文件信息列表（按时间倒序，最新的在前），如果模块不存在或没有日志文件则返回空列表
     */
    fun getLogFileInfos(moduleName: String): List<LogFileInfo>
    
    /**
     * 获取指定模块在指定时间范围内的日志文件信息列表
     * 用于业务场景：上报指定时间段的日志
     * 
     * @param moduleName 模块名
     * @param daysAgo 多少天前的日志（0 表示今天，1 表示昨天，以此类推）
     * @return 日志文件信息列表（按时间倒序，最新的在前）
     */
    fun getLogFileInfos(moduleName: String, daysAgo: Int): List<LogFileInfo>
    
    /**
     * 获取指定模块在指定时间范围内的日志文件信息列表
     * 用于业务场景：上报指定时间段的日志
     * 
     * @param moduleName 模块名
     * @param startTime 开始时间（毫秒时间戳）
     * @param endTime 结束时间（毫秒时间戳）
     * @return 日志文件信息列表（按时间倒序，最新的在前）
     */
    fun getLogFileInfos(moduleName: String, startTime: Long, endTime: Long): List<LogFileInfo>
    
    /**
     * 异步获取指定模块在指定时间范围内的日志文件信息列表
     * 使用 Kotlin 协程实现，完全依赖调用方的协程作用域
     * 
     * **设计说明**：
     * - 不创建新的协程作用域，完全由调用方管理协程生命周期
     * - 调用方可以在自己的协程作用域中调用，便于统一管理和监控
     * - 内部只做必要的线程切换（IO 操作），不干预协程作用域
     * 
     * **Kotlin 代码推荐使用此方法**
     * 
     * @param moduleName 模块名
     * @param startTime 开始时间（毫秒时间戳）
     * @param endTime 结束时间（毫秒时间戳）
     * @return 日志文件信息列表（按时间倒序，最新的在前）
     */
    suspend fun getLogFileInfosAsync(
        moduleName: String, 
        startTime: Long, 
        endTime: Long
    ): List<LogFileInfo>
    
    /**
     * 异步获取指定模块在指定时间范围内的日志文件信息列表（Java 兼容版本）
     * 使用回调接口返回结果，支持自定义执行环境
     * 
     * **设计说明**：
     * - 提供 Executor 参数（Java 友好），让使用方指定执行线程
     * - 如果不提供 Executor，使用默认的协程作用域（向后兼容）
     * - Kotlin 代码如需指定协程作用域，请使用扩展函数版本（XLogLoggerExtensions.kt）
     * 
     * **Java 代码推荐使用此方法**
     * 
     * @param moduleName 模块名
     * @param startTime 开始时间（毫秒时间戳）
     * @param endTime 结束时间（毫秒时间戳）
     * @param callback 查询结果回调
     * @param executor 执行线程池（可选，Java 友好）。如果提供，将在该线程池中执行任务，回调也在该线程池中执行
     */
    fun getLogFileInfosAsync(
        moduleName: String,
        startTime: Long,
        endTime: Long,
        callback: LogFileInfoCallback,
        executor: java.util.concurrent.Executor?
    )
    
    /**
     * 异步获取指定模块在指定时间范围内的日志文件信息列表（Java 兼容版本，无 Executor）
     * 使用默认的协程作用域执行（向后兼容）
     * 
     * @param moduleName 模块名
     * @param startTime 开始时间（毫秒时间戳）
     * @param endTime 结束时间（毫秒时间戳）
     * @param callback 查询结果回调
     */
    fun getLogFileInfosAsync(
        moduleName: String,
        startTime: Long,
        endTime: Long,
        callback: LogFileInfoCallback
    )
    
    /**
     * 批量刷新多个模块的日志缓冲区
     * 用于业务场景：上报多个模块的日志前，批量刷新确保日志已落盘
     * 
     * @param moduleNames 模块名列表
     * @param isSync 是否同步刷新（true：同步，确保立即写入；false：异步，通知后台线程刷新）
     * @return 成功刷新的模块名列表
     */
    fun flushModules(moduleNames: List<String>, isSync: Boolean = true): List<String>
    
    /**
     * 获取所有已注册的模块名列表
     * 用于业务场景：获取所有需要上报日志的模块
     * 
     * @return 模块名列表
     */
    fun getAllModules(): List<String>
    
    /**
     * 清除指定模块的文件列表缓存
     * 用于业务场景：文件被外部删除或修改后，需要刷新缓存
     * 
     * @param moduleName 模块名
     */
    fun clearFileCache(moduleName: String)
    
    /**
     * 清除所有模块的文件列表缓存
     */
    fun clearAllFileCache()
    
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

