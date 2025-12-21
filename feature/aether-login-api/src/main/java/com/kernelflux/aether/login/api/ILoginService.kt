package com.kernelflux.aether.login.api

import android.app.Activity
import android.content.Context

/**
 * 用户信息
 */
data class UserInfo(
    val userId: String,
    val nickname: String,
    val avatar: String? = null,
    val token: String? = null,
    val extraInfo: Map<String, String> = emptyMap()
)

/**
 * 登录结果回调
 */
interface LoginCallback {
    fun onSuccess(userInfo: UserInfo)
    fun onError(error: Throwable)
    fun onCancel()
}

/**
 * 登录服务接口
 * 
 * @author Aether Framework
 */
interface ILoginService {
    
    /**
     * 发起登录
     * @param activity 当前Activity
     * @param callback 登录结果回调
     */
    fun login(
        activity: Activity,
        callback: LoginCallback
    )
    
    /**
     * 退出登录
     */
    fun logout()
    
    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean
    
    /**
     * 获取当前用户信息
     */
    fun getCurrentUser(): UserInfo?
    
    /**
     * 检查登录方式是否可用
     */
    fun isAvailable(): Boolean
    
    /**
     * 获取登录方式名称
     * 
     * @param context 上下文（可选，用于获取本地化字符串）
     */
    fun getLoginName(context: Context? = null): String {
        // 默认实现：返回默认名称
        // 子类可以重写以提供更具体的名称
        // 应用层应该在res/values/strings.xml中定义对应的资源
        return "OAuth"
    }
}

