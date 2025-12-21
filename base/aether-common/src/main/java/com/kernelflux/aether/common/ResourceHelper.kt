package com.kernelflux.aether.common

import android.content.Context

/**
 * 资源获取辅助类
 * 
 * 使用Android标准的Resources系统获取字符串资源
 * 
 * 使用方式：
 * ```kotlin
 * ResourceHelper.getString(context, "payment_success", "Payment successful")
 * ```
 * 
 * 注意：应用层需要在 res/values/strings.xml 中定义对应的资源
 * 
 * @author Aether Framework
 */
object ResourceHelper {
    
    /**
     * 获取字符串资源
     * 
     * @param context Android上下文
     * @param resourceName 资源名称（如 "payment_success"）
     * @param default 默认值（如果资源不存在时返回）
     * @return 字符串资源
     */
    fun getString(context: Context?, resourceName: String, default: String): String {
        if (context == null) return default
        
        return try {
            val resId = context.resources.getIdentifier(
                resourceName,
                "string",
                context.packageName
            )
            if (resId != 0) {
                context.getString(resId)
            } else {
                default
            }
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * 获取格式化字符串资源
     * 
     * @param context Android上下文
     * @param resourceName 资源名称
     * @param default 默认值（支持格式化，如 "Payment %s"）
     * @param args 格式化参数
     * @return 格式化后的字符串
     */
    fun getString(context: Context?, resourceName: String, default: String, vararg args: Any?): String {
        val template = getString(context, resourceName, default)
        return try {
            if (args.isEmpty()) {
                template
            } else {
                String.format(template, *args)
            }
        } catch (e: Exception) {
            template
        }
    }
}
