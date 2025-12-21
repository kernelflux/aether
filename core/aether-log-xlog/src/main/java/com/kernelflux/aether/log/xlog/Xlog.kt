package com.kernelflux.aether.log.xlog

import android.os.Looper
import android.os.Process

/**
 * Xlog native wrapper for Aether xlog library
 *
 * 纯 native 方法桥接，不包含业务逻辑
 * 所有业务逻辑由 XLogProvider 处理
 */
object Xlog {

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
     * @param isLoadLib Whether to load native libraries (should be true)
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
        isLoadLib: Boolean,
        level: Int,
        mode: Int,
        cacheDir: String,
        logDir: String,
        namePrefix: String,
        cacheDays: Int = 0,
        pubkey: String? = null,
        isCompress: Boolean = true
    ) {
        if (isLoadLib) {
            try {
                System.loadLibrary("c++_shared")
            } catch (e: UnsatisfiedLinkError) {
                // c++_shared may already be loaded by another library
            }
            System.loadLibrary("aetherxlog")
        }
        appenderOpen(level, mode, cacheDir, logDir, namePrefix, cacheDays, pubkey, isCompress)
    }

    /**
     * 写入日志
     * @param level 日志级别
     * @param tag 标签
     * @param message 日志消息
     */
    @JvmStatic
    fun log(level: Int, tag: String, message: String) {
        logWrite2(
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

    @JvmStatic
    external fun logWrite2(
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

    @JvmStatic
    external fun appenderFlushNative(isSync: Boolean)

    /**
     * 设置自定义日志头部信息
     * @param headerInfo 自定义信息，每行一个键值对，例如："Device: Pixel 5\nIP: 192.168.1.100"
     */
    @JvmStatic
    external fun setCustomHeaderInfo(headerInfo: String?)
}
