package com.kernelflux.aether.share.api

import android.app.Activity
import android.content.Context

/**
 * 分享内容类型
 */
enum class ShareType {
    TEXT,
    IMAGE,
    LINK,
    VIDEO,
    FILE
}

/**
 * 分享内容
 */
data class ShareContent(
    val type: ShareType,
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val linkUrl: String? = null,
    val filePath: String? = null
)

/**
 * 分享结果回调
 */
interface ShareCallback {
    fun onSuccess()
    fun onError(error: Throwable)
    fun onCancel()
}

/**
 * 分享服务接口
 * 
 * @author Aether Framework
 */
interface IShareService {
    
    /**
     * 分享内容
     * @param activity 当前Activity
     * @param content 分享内容
     * @param callback 分享结果回调
     */
    fun share(
        activity: Activity,
        content: ShareContent,
        callback: ShareCallback? = null
    )
    
    /**
     * 检查分享方式是否可用
     */
    fun isAvailable(): Boolean
    
    /**
     * 获取分享方式名称
     * 
     * @param context 上下文（可选，用于获取本地化字符串）
     */
    fun getShareName(context: Context? = null): String {
        // 默认实现：返回默认名称
        // 子类可以重写以提供更具体的名称
        // 应用层应该在res/values/strings.xml中定义对应的资源
        return "Share"
    }
}

