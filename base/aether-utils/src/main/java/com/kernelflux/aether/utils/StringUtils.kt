package com.kernelflux.aether.utils

/**
 * 字符串工具类
 * 
 * 提供常用的字符串操作方法
 * 
 * @author Aether Framework
 */
object StringUtils {
    
    /**
     * 判断字符串是否为空
     */
    fun isEmpty(str: String?): Boolean {
        return str == null || str.isEmpty()
    }
    
    /**
     * 判断字符串是否不为空
     */
    fun isNotEmpty(str: String?): Boolean {
        return !isEmpty(str)
    }
    
    /**
     * 判断字符串是否为空白（包括空格、制表符等）
     */
    fun isBlank(str: String?): Boolean {
        return str == null || str.isBlank()
    }
    
    /**
     * 判断字符串是否不为空白
     */
    fun isNotBlank(str: String?): Boolean {
        return !isBlank(str)
    }
    
    /**
     * 安全获取字符串，如果为空则返回默认值
     */
    fun defaultIfEmpty(str: String?, default: String): String {
        return if (isEmpty(str)) default else str!!
    }
    
    /**
     * 安全获取字符串，如果为空白则返回默认值
     */
    fun defaultIfBlank(str: String?, default: String): String {
        return if (isBlank(str)) default else str!!
    }
    
    /**
     * 截取字符串，如果超过长度则截取并添加后缀
     */
    fun truncate(str: String, maxLength: Int, suffix: String = "..."): String {
        return if (str.length <= maxLength) {
            str
        } else {
            str.substring(0, maxLength - suffix.length) + suffix
        }
    }
    
    /**
     * 首字母大写
     */
    fun capitalize(str: String): String {
        return if (isEmpty(str)) {
            str
        } else {
            str[0].uppercaseChar() + str.substring(1)
        }
    }
    
    /**
     * 首字母小写
     */
    fun uncapitalize(str: String): String {
        return if (isEmpty(str)) {
            str
        } else {
            str[0].lowercaseChar() + str.substring(1)
        }
    }
    
    /**
     * 驼峰命名转下划线命名
     */
    fun camelToUnderscore(str: String): String {
        return str.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }
    
    /**
     * 下划线命名转驼峰命名
     */
    fun underscoreToCamel(str: String): String {
        val parts = str.split("_")
        return parts[0] + parts.drop(1).joinToString("") { capitalize(it) }
    }
}
