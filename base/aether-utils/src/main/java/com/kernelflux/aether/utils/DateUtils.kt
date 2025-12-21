package com.kernelflux.aether.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期时间工具类
 * 
 * 提供常用的日期时间操作方法
 * 
 * @author Aether Framework
 */
object DateUtils {
    
    /**
     * 格式化日期
     */
    fun format(date: Date?, pattern: String): String {
        if (date == null) return ""
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }
    
    /**
     * 格式化日期（默认格式：yyyy-MM-dd HH:mm:ss）
     */
    fun format(date: Date?): String {
        return format(date, "yyyy-MM-dd HH:mm:ss")
    }
    
    /**
     * 格式化当前时间
     */
    fun formatNow(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return format(Date(), pattern)
    }
    
    /**
     * 解析日期字符串
     */
    fun parse(dateStr: String, pattern: String): Date? {
        return try {
            SimpleDateFormat(pattern, Locale.getDefault()).parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取时间戳（毫秒）
     */
    fun getTimestamp(date: Date? = null): Long {
        return (date ?: Date()).time
    }
    
    /**
     * 获取当前时间戳（毫秒）
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * 时间戳转Date
     */
    fun timestampToDate(timestamp: Long): Date {
        return Date(timestamp)
    }
    
    /**
     * 判断是否为今天
     */
    fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * 判断是否为昨天
     */
    fun isYesterday(date: Date): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val target = Calendar.getInstance().apply { time = date }
        return yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * 获取两个日期之间的天数差
     */
    fun daysBetween(date1: Date, date2: Date): Long {
        val diff = Math.abs(date1.time - date2.time)
        return diff / (24 * 60 * 60 * 1000)
    }
}
