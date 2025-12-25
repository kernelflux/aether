package com.kernelflux.aether.log.xlog

import android.os.Looper
import android.os.Process
import com.kernelflux.aether.log.api.LibraryLoader

/**
 * Xlog native wrapper for Aether xlog library
 *
 * 纯 native 方法桥接，不包含业务逻辑
 * 所有业务逻辑由 XLogProvider 处理
 */
object Xlog {

    /**
     * 自定义 so 库加载器（可选）
     * 如果设置，将使用自定义加载器加载 so 库；否则使用 System.loadLibrary
     */
    @Volatile
    private var libraryLoader: LibraryLoader? = null

    /**
     * 设置自定义 so 库加载器
     * 用于支持应用层使用自定义的 so 加载器，如 ReLinker
     *
     * @param loader 自定义加载器，如果为 null 则使用默认的 System.loadLibrary
     */
    @JvmStatic
    fun setLibraryLoader(loader: LibraryLoader?) {
        libraryLoader = loader
    }

    /**
     * 加载 so 库
     * 优先使用自定义加载器，如果没有设置则使用 System.loadLibrary
     */
    private fun loadLibrary(libName: String) {
        val loader = libraryLoader
        if (loader != null) {
            loader.loadLibrary(libName)
        } else {
            System.loadLibrary(libName)
        }
    }

    /**
     * 内部方法：加载 so 库（供 XLogLogger 使用）
     * 优先使用自定义加载器，如果没有设置则使用 System.loadLibrary
     */
    @JvmStatic
    internal fun loadLibraryInternal(libName: String) {
        loadLibrary(libName)
    }

    const val LEVEL_ALL = 0
    const val LEVEL_VERBOSE = 0
    const val LEVEL_DEBUG = 1
    const val LEVEL_INFO = 2
    const val LEVEL_WARNING = 3
    const val LEVEL_ERROR = 4
    const val LEVEL_FATAL = 5
    const val LEVEL_NONE = 6

    const val APPENDER_MODE_ASYNC = 0
    const val APPENDER_MODE_SYNC = 1

    /**
     * Initialize xlog
     * @param level Log level
     * @param mode Appender mode (async or sync)
     * @param cacheDir Cache directory path
     * @param logDir Log directory path
     * @param namePrefix Log file name prefix
     * @param cacheDays Cache log retention days (0 means no cache)
     * @param pubkey Public key for encryption (can be null)
     * @param isCompress Whether to compress log files (default true)
     */
    @JvmStatic
    fun open(
        level: Int,
        mode: Int,
        cacheDir: String,
        logDir: String,
        namePrefix: String,
        cacheDays: Int = 0,
        pubkey: String? = null,
        isCompress: Boolean = true
    ) {
        appenderOpen(level, mode, cacheDir, logDir, namePrefix, cacheDays, pubkey, isCompress)
    }

    /**
     * 写入日志（使用默认实例，instancePtr = 0）
     * @param level 日志级别
     * @param tag 标签
     * @param message 日志消息
     */
    @JvmStatic
    fun log(level: Int, tag: String, message: String) {
        logWrite2(
            instancePtr = 0L,
            level = level,
            tag = tag,
            filename = "",
            funcname = "",
            line = 0,
            pid = Process.myPid(),
            tid = Process.myTid().toLong(),
            maintid = Looper.getMainLooper().thread.threadId(),
            log = message
        )
    }

    /**
     * 写入日志（支持实例指针）
     * @param instancePtr 实例指针，0 表示使用默认全局实例
     * @param level 日志级别
     * @param tag 标签
     * @param message 日志消息
     */
    @JvmStatic
    fun log(instancePtr: Long, level: Int, tag: String, message: String) {
        // 获取调用位置信息（跳过 Xlog.log 和 XLogLogger.logInternal 这两层）
        val stackTrace = Throwable().stackTrace
        val callerIndex = stackTrace.indexOfFirst {
            !it.className.contains("Xlog") &&
                    !it.className.contains("XLogLogger") &&
                    !it.className.contains("LoggerHelper")
        }

        val filename = if (callerIndex >= 0 && callerIndex < stackTrace.size) {
            val className = stackTrace[callerIndex].className
            val simpleName = className.substringAfterLast('.')
            "$simpleName.kt"
        } else {
            ""
        }

        val funcname = if (callerIndex >= 0 && callerIndex < stackTrace.size) {
            stackTrace[callerIndex].methodName
        } else {
            ""
        }

        val line = if (callerIndex >= 0 && callerIndex < stackTrace.size) {
            stackTrace[callerIndex].lineNumber
        } else {
            0
        }

        logWrite2(
            instancePtr = instancePtr,
            level = level,
            tag = tag,
            filename = filename,
            funcname = funcname,
            line = line,
            pid = Process.myPid(),
            tid = Process.myTid().toLong(),
            maintid = getMainThreadId(),
            log = message
        )
    }

    private fun getMainThreadId(): Long {
        return try {
            Looper.getMainLooper().thread.threadId()
        } catch (_: Throwable) {
            Looper.getMainLooper().thread.id
        }
    }

    @JvmStatic
    external fun logWrite2(
        instancePtr: Long,
        level: Int,
        tag: String,
        filename: String,
        funcname: String,
        line: Int,
        pid: Int,
        tid: Long,
        maintid: Long,
        log: String
    )

    /**
     * 创建新的 xlog 实例
     * @param level 日志级别
     * @param mode Appender 模式
     * @param cacheDir 缓存目录
     * @param logDir 日志目录
     * @param namePrefix 文件名前缀（用于区分不同模块）
     * @param cacheDays 缓存天数
     * @param pubkey 加密公钥（可选）
     * @param isCompress 是否压缩
     * @return 实例指针，0 表示创建失败
     */
    @JvmStatic
    external fun newXlogInstance(
        level: Int,
        mode: Int,
        cacheDir: String,
        logDir: String,
        namePrefix: String,
        cacheDays: Int,
        pubkey: String?,
        isCompress: Boolean
    ): Long

    /**
     * 获取已存在的 xlog 实例
     * @param namePrefix 文件名前缀
     * @return 实例指针，0 表示实例不存在
     */
    @JvmStatic
    external fun getXlogInstance(namePrefix: String): Long

    /**
     * 释放 xlog 实例
     * @param namePrefix 文件名前缀
     */
    @JvmStatic
    external fun releaseXlogInstance(namePrefix: String)

    @JvmStatic
    external fun getLogLevelNative(): Int

    @JvmStatic
    external fun setLogLevel(logLevel: Int)

    @JvmStatic
    external fun setAppenderMode(mode: Int)

    @JvmStatic
    external fun setConsoleLogOpen(isOpen: Boolean)

    @JvmStatic
    external fun appenderOpen(
        level: Int,
        mode: Int,
        cacheDir: String,
        logDir: String,
        namePrefix: String,
        cacheDays: Int,
        pubkey: String?,
        isCompress: Boolean
    )

    @JvmStatic
    external fun setMaxFileSize(size: Long)

    @JvmStatic
    external fun setMaxAliveTime(duration: Long)

    @JvmStatic
    external fun appenderCloseNative()

    /**
     * 刷新所有日志实例（包括默认实例和所有模块实例）
     * @param isSync 是否同步刷新（true：同步，确保立即写入；false：异步，通知后台线程刷新）
     */
    @JvmStatic
    external fun flushAll(isSync: Boolean)

    /**
     * 刷新指定模块的日志缓冲区
     * @param moduleName 模块名
     * @param isSync 是否同步刷新（true：同步，确保立即写入；false：异步，通知后台线程刷新）
     */
    @JvmStatic
    external fun flushModule(moduleName: String, isSync: Boolean)

    /**
     * 获取指定模块的日志文件路径列表
     * @param moduleName 模块名
     * @return 日志文件路径列表（包括 logDir 和 cacheDir 中的文件，如果存在）
     */
    @JvmStatic
    external fun getLogFiles(moduleName: String): Array<String>

    /**
     * 获取指定模块的日志文件信息列表
     * @param moduleName 模块名
     * @return 日志文件信息列表（按时间倒序，最新的在前）
     */
    @JvmStatic
    external fun getLogFileInfos(moduleName: String): Array<com.kernelflux.aether.log.api.LogFileInfo>

    /**
     * 获取指定模块在指定天数前的日志文件信息列表
     * @param moduleName 模块名
     * @param daysAgo 多少天前的日志（0 表示今天，1 表示昨天，以此类推）
     * @return 日志文件信息列表（按时间倒序，最新的在前）
     */
    @JvmStatic
    external fun getLogFileInfosByDays(
        moduleName: String,
        daysAgo: Int
    ): Array<com.kernelflux.aether.log.api.LogFileInfo>

    /**
     * 获取指定模块在指定时间范围内的日志文件信息列表
     * @param moduleName 模块名
     * @param startTime 开始时间（毫秒时间戳）
     * @param endTime 结束时间（毫秒时间戳）
     * @return 日志文件信息列表（按时间倒序，最新的在前）
     */
    @JvmStatic
    external fun getLogFileInfosByTimeRange(
        moduleName: String,
        startTime: Long,
        endTime: Long
    ): Array<com.kernelflux.aether.log.api.LogFileInfo>

    /**
     * 清除指定模块的文件列表缓存
     * @param moduleName 模块名
     */
    @JvmStatic
    external fun clearFileCache(moduleName: String)

    /**
     * 清除所有模块的文件列表缓存
     */
    @JvmStatic
    external fun clearAllFileCache()

    /**
     * 设置自定义日志头部信息
     * @param headerInfo 自定义信息，每行一个键值对，例如："Device: Pixel 5\nIP: 192.168.1.100"
     */
    @JvmStatic
    external fun setCustomHeaderInfo(headerInfo: String?)
}
