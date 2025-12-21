package com.kernelflux.aether.log.api

import android.util.Log

/**
 * 日志工具类
 * 提供无状态的通用日志处理逻辑
 * 
 * 符合 SPI 原则：工具类不违反 API 模块的纯净性
 * 
 * @author Aether Framework
 */
object LoggerHelper {
    
    /**
     * 判断是否应该输出日志
     * 
     * @param level 当前日志级别
     * @param enabled 是否启用日志
     * @param currentLevel 当前设置的日志级别
     * @return true 如果应该输出，false 否则
     */
    fun shouldLog(level: LogLevel, enabled: Boolean, currentLevel: LogLevel): Boolean {
        if (!enabled) return false
        return level.ordinal >= currentLevel.ordinal
    }
    
    /**
     * 格式化消息，处理 throwable
     * 
     * @param message 原始消息
     * @param throwable 异常对象（可选）
     * @return 格式化后的消息
     */
    fun formatMessage(message: String, throwable: Throwable?): String {
        return if (throwable != null) {
            "$message\n${Log.getStackTraceString(throwable)}"
        } else {
            message
        }
    }
    
    /**
     * 格式化 tag（添加前缀）
     * 
     * @param tag 原始 tag
     * @param prefix 前缀（可选）
     * @return 格式化后的 tag
     */
    fun formatTag(tag: String, prefix: String?): String {
        return if (prefix != null && prefix.isNotEmpty()) {
            "$prefix:$tag"
        } else {
            tag
        }
    }
}

