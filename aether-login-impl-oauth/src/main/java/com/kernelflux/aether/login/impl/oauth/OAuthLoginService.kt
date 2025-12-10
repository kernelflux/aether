package com.kernelflux.aether.login.impl.oauth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.kernelflux.aether.login.spi.ILoginService
import com.kernelflux.aether.login.spi.LoginCallback
import com.kernelflux.aether.login.spi.UserInfo
import com.kernelflux.fluxrouter.annotation.FluxService

/**
 * OAuth登录实现（示例）
 * 实际使用时需要集成具体的OAuth SDK
 * 
 * @author Aether Framework
 */
@FluxService(interfaceClass = ILoginService::class)
class OAuthLoginService : ILoginService {
    
    private lateinit var context: Context
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("aether_login", Context.MODE_PRIVATE)
    }

    override fun login(
        activity: Activity,
        callback: LoginCallback
    ) {
        // TODO: 实现OAuth登录逻辑
        // 这里只是示例，实际需要调用OAuth SDK
        try {
            // 模拟登录成功
            val userInfo = UserInfo(
                userId = "user_123",
                nickname = "测试用户",
                avatar = "https://example.com/avatar.jpg",
                token = "token_123456"
            )
            
            // 保存用户信息
            prefs.edit()
                .putString("user_id", userInfo.userId)
                .putString("user_token", userInfo.token)
                .apply()
            
            callback.onSuccess(userInfo)
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
    
    override fun logout() {
        prefs.edit().clear().apply()
    }
    
    override fun isLoggedIn(): Boolean {
        return prefs.contains("user_id")
    }
    
    override fun getCurrentUser(): UserInfo? {
        if (!isLoggedIn()) return null
        
        return UserInfo(
            userId = prefs.getString("user_id", "") ?: "",
            nickname = prefs.getString("user_nickname", "") ?: "",
            token = prefs.getString("user_token", "")
        )
    }
    
    override fun isAvailable(): Boolean {
        return true
    }
    
    override fun getLoginName(): String = "OAuth"
}

