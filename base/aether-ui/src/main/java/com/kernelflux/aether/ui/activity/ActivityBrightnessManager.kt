package com.kernelflux.aether.ui.activity

import android.app.Activity
import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.annotation.IntRange

/**
 * Activity 屏幕亮度管理器
 * 
 * 提供统一的屏幕亮度管理功能
 * 支持设置 Activity 亮度、恢复系统亮度、监听系统亮度变化
 */
object ActivityBrightnessManager {
    private const val MAX_BRIGHTNESS = 255
    private const val SYSTEM_DEF_BRIGHTNESS = 125
    
    private var appContentResolver: ContentResolver? = null
    private var brightnessObserver: ContentObserver? = null
    private var currentBrightness: Int = -1
    
    /**
     * 初始化（在 Application.onCreate 中调用）
     */
    fun init(contentResolver: ContentResolver) {
        appContentResolver = contentResolver
    }
    
    /**
     * 获取当前 Activity 的亮度
     * 
     * @return 亮度值（0-255），-1 表示使用系统亮度
     */
    fun getActivityBrightness(): Int {
        if (currentBrightness >= 0) {
            return currentBrightness
        }
        
        val topActivity = ActivityStackManager.getTopActivity() ?: return getSystemBrightness()
        
        return try {
            val attributes = topActivity.window.attributes
            if (needSystemBrightness(attributes)) {
                getSystemBrightness()
            } else {
                (attributes.screenBrightness * MAX_BRIGHTNESS).toInt()
            }
        } catch (e: Exception) {
            getSystemBrightness()
        }
    }
    
    /**
     * 获取系统亮度
     * 
     * @return 亮度值（0-255）
     */
    fun getSystemBrightness(): Int {
        return try {
            appContentResolver?.let {
                Settings.System.getInt(it, Settings.System.SCREEN_BRIGHTNESS, SYSTEM_DEF_BRIGHTNESS)
            } ?: SYSTEM_DEF_BRIGHTNESS
        } catch (e: Exception) {
            SYSTEM_DEF_BRIGHTNESS
        }
    }
    
    /**
     * 获取最大亮度值
     */
    fun getMaxBrightness(): Int = MAX_BRIGHTNESS
    
    /**
     * 设置 Activity 亮度
     * 
     * @param brightness 亮度值（0-255）
     */
    @Synchronized
    fun setActivityBrightness(@IntRange(from = 0, to = 255) brightness: Int) {
        currentBrightness = brightness.coerceIn(0, MAX_BRIGHTNESS)
        
        val topActivity = ActivityStackManager.getTopActivity()
        if (topActivity != null) {
            updateActivityBrightness(topActivity)
            registerScreenBrightnessObserver()
        }
    }
    
    /**
     * 更新指定 Activity 的亮度
     */
    fun updateActivityBrightness(activity: Activity?) {
        if (activity == null || currentBrightness < 0) {
            return
        }
        
        try {
            val window = activity.window
            val attributes = window.attributes
            attributes.screenBrightness = (currentBrightness.toFloat() / MAX_BRIGHTNESS.toFloat()).coerceIn(0f, 1f)
            window.attributes = attributes
        } catch (e: Exception) {
            // 防止设置亮度异常
        }
    }
    
    /**
     * 恢复所有 Activity 到系统亮度
     */
    fun restoreAppToSystemBrightness() {
        currentBrightness = -1
        
        ActivityStackManager.getActivityStack().forEach { activity ->
            if (!activity.isFinishing) {
                try {
                    val window = activity.window
                    val attributes = window.attributes
                    attributes.screenBrightness = -1f
                    window.attributes = attributes
                } catch (e: Exception) {
                    // 防止恢复亮度异常
                }
            }
        }
    }
    
    /**
     * 注册系统亮度变化监听器
     */
    private fun registerScreenBrightnessObserver() {
        if (brightnessObserver == null && appContentResolver != null) {
            brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    if (currentBrightness >= 0) {
                        restoreAppToSystemBrightness()
                    }
                    currentBrightness = -1
                }
            }.also {
                try {
                    appContentResolver?.registerContentObserver(
                        Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                        true,
                        it
                    )
                } catch (e: Exception) {
                    // 防止注册监听器异常
                }
            }
        }
    }
    
    /**
     * 注销系统亮度变化监听器
     */
    fun unregisterScreenBrightnessObserver() {
        brightnessObserver?.let {
            try {
                appContentResolver?.unregisterContentObserver(it)
            } catch (e: Exception) {
                // 防止注销监听器异常
            }
            brightnessObserver = null
        }
    }
    
    /**
     * 判断是否需要使用系统亮度
     */
    private fun needSystemBrightness(layoutParams: WindowManager.LayoutParams): Boolean {
        val brightness = layoutParams.screenBrightness
        return brightness == -1.0f || brightness == 0.0f
    }
}

