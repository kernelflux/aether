package com.kernelflux.aether.log.impl.android

import android.util.Log
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.fluxrouter.annotation.FluxService

/**
 * Android系统日志实现
 *
 * @author Aether Framework
 */
@FluxService(interfaceClass = ILogger::class)
class AndroidLogger : ILogger {

    private var logLevel: LogLevel = LogLevel.DEBUG
    private var enabled: Boolean = true

    override fun setLogLevel(level: LogLevel) {
        this.logLevel = level
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun v(tag: String, message: String, throwable: Throwable?) {
        if (enabled && logLevel.ordinal <= LogLevel.VERBOSE.ordinal) {
            if (throwable != null) {
                Log.v(tag, message, throwable)
            } else {
                Log.v(tag, message)
            }
        }
    }

    override fun d(tag: String, message: String, throwable: Throwable?) {
        if (enabled && logLevel.ordinal <= LogLevel.DEBUG.ordinal) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }

    override fun i(tag: String, message: String, throwable: Throwable?) {
        if (enabled && logLevel.ordinal <= LogLevel.INFO.ordinal) {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (enabled && logLevel.ordinal <= LogLevel.WARN.ordinal) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (enabled && logLevel.ordinal <= LogLevel.ERROR.ordinal) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
}

