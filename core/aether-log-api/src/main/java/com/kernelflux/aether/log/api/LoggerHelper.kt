package com.kernelflux.aether.log.api

import android.util.Log

/**
 * 日志工具类
 * 提供无状态的通用日志处理逻辑
 * 
 * 符合 SPI 原则：工具类不违反 API 模块的纯净性
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
     * 包含异常处理，防止非法字符导致crash
     * 
     * @param message 原始消息
     * @param throwable 异常对象（可选）
     * @return 格式化后的消息
     */
    fun formatMessage(message: String, throwable: Throwable?): String {
        return try {
            val safeMessage = sanitizeMessage(message)
            if (throwable != null) {
                val stackTrace = try {
                    Log.getStackTraceString(throwable)
                } catch (e: Exception) {
                    "Failed to get stack trace: ${e.message}"
                }
                "$safeMessage\n$stackTrace"
            } else {
                safeMessage
            }
        } catch (e: Exception) {
            "Failed to format message: ${e.message}"
        }
    }
    
    /**
     * 清理消息中的非法字符，防止导致crash
     * 
     * @param message 原始消息
     * @return 清理后的消息
     */
    private fun sanitizeMessage(message: String): String {
        return try {
            if (message.isEmpty()) {
                return message
            }
            // 移除或替换可能导致问题的控制字符
            // 保留换行符和制表符，但移除其他控制字符
            message.replace(Regex("[\u0000-\u0008\u000B\u000C\u000E-\u001F]"), "")
        } catch (e: Exception) {
            "Invalid message format"
        }
    }
    
    /**
     * 格式化 tag（添加前缀）
     * 包含异常处理，防止非法字符导致crash
     * 
     * @param tag 原始 tag
     * @param prefix 前缀（可选）
     * @return 格式化后的 tag
     */
    fun formatTag(tag: String, prefix: String?): String {
        return try {
            val safeTag = sanitizeTag(tag)
            if (prefix != null && prefix.isNotEmpty()) {
                val safePrefix = sanitizeTag(prefix)
                "$safePrefix:$safeTag"
            } else {
                safeTag
            }
        } catch (e: Exception) {
            "InvalidTag"
        }
    }
    
    /**
     * 清理tag中的非法字符，防止导致crash
     * 
     * @param tag 原始tag
     * @return 清理后的tag
     */
    private fun sanitizeTag(tag: String): String {
        return try {
            if (tag.isEmpty()) {
                return "Unknown"
            }
            // 移除可能导致问题的字符，保留字母、数字、下划线、连字符
            tag.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
        } catch (e: Exception) {
            "InvalidTag"
        }
    }

    /**
     * 安全的日志打印方法
     * 即使原生日志失败，也会尝试使用System.err输出错误信息
     * 确保使用方能够感知到日志系统的问题
     * 
     * @param tag 日志标签
     * @param message 日志消息
     * @param throwable 异常对象（可选）
     */
    fun safeLogError(tag: String, message: String, throwable: Throwable? = null) {
        try {
            // 首先尝试使用Android Log
            Log.e(tag, message, throwable)
        } catch (e: Exception) {
            // 如果Android Log也失败了，使用System.err作为最后的兜底
            try {
                System.err.println("[$tag] $message")
                throwable?.printStackTrace(System.err)
                e.printStackTrace(System.err)
            } catch (ignored: Exception) {
                // 如果连System.err都失败了，只能忽略
                // 这种情况极其罕见，通常表示系统资源严重不足
            }
        }
    }
}

