package com.kernelflux.aether.ui.activity

import android.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope

/**
 * 基础Activity
 */
abstract class BaseActivity : AppCompatActivity(), IActivityStack {
    
    // ==================== 代理管理 ====================
    
    /**
     * Activity 代理管理器
     */
    private val proxyManager = ActivityProxyManager()
    
    /**
     * 注册生命周期代理
     * 
     * @param proxy 代理实例
     */
    protected fun registerProxy(proxy: ActivityProxy) {
        proxyManager.register(proxy)
    }
    
    /**
     * 注销生命周期代理
     * 
     * @param proxy 代理实例
     */
    protected fun unregisterProxy(proxy: ActivityProxy) {
        proxyManager.unregister(proxy)
    }
    
    // ==================== 生命周期管理 ====================
    
    /**
     * Activity 是否已创建（onCreate 已调用）
     */
    protected var isCreated: Boolean = false
        private set
    
    /**
     * Activity 是否可见（onStart 已调用）
     */
    protected var isVisible: Boolean = false
        private set
    
    /**
     * Activity 是否在前台（onResume 已调用）
     */
    protected var isForeground: Boolean = false
        private set
    
    /**
     * 协程作用域（自动绑定生命周期）
     */
    protected val activityScope: CoroutineScope
        get() = lifecycleScope
    
    // ==================== EdgeToEdge 配置 ====================
    
    /**
     * 是否启用 EdgeToEdge 模式
     * 子类可重写此方法返回 false 来禁用 EdgeToEdge
     */
    protected open fun enableEdgeToEdge(): Boolean = true
    
    /**
     * 是否适配状态栏
     * 子类可重写此方法返回 false 来禁用状态栏适配
     */
    protected open fun fitStatusBar(): Boolean = true
    
    /**
     * 是否适配导航栏
     * 子类可重写此方法返回 false 来禁用导航栏适配
     */
    protected open fun fitNavigationBar(): Boolean = true
    
    /**
     * 状态栏文字是否为浅色（白色文字，适配深色背景）
     * 子类可重写此方法来自定义状态栏文字颜色
     */
    protected open fun isLightStatusBar(): Boolean = false
    
    /**
     * 导航栏文字是否为浅色（白色文字，适配深色背景）
     * 子类可重写此方法来自定义导航栏文字颜色
     */
    protected open fun isLightNavigationBar(): Boolean = false
    
    // ==================== 布局管理 ====================
    
    /**
     * 获取布局资源ID
     * 子类必须实现此方法返回布局资源ID
     * 
     * 如果返回 0，则不会自动设置布局，子类需要在 initView() 中手动调用 setContentView()
     */
    @LayoutRes
    protected abstract fun getLayoutId(): Int
    
    // ==================== 初始化方法 ====================
    
    /**
     * 初始化视图
     * 子类应在此方法中初始化视图组件、设置 ViewBinding 等
     * 
     * 注意：如果 getLayoutId() 返回非 0，布局会在调用此方法前自动设置
     */
    @CallSuper
    protected open fun initView() {
        // 子类实现
    }
    
    /**
     * 初始化数据
     * 子类应在此方法中初始化数据、从 Intent 获取参数等
     */
    @CallSuper
    protected open fun initData() {
        // 子类实现
    }
    
    /**
     * 初始化监听器
     * 子类应在此方法中设置事件监听、点击事件等
     */
    @CallSuper
    protected open fun initListener() {
        // 子类实现
    }
    
    // ==================== 生命周期回调 ====================
    
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        proxyManager.attachBaseContext(this, newBase)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            
            // 代理 onCreate
            proxyManager.onCreate(this, savedInstanceState)
            
            // 配置 EdgeToEdge（异常保护）
            if (enableEdgeToEdge()) {
                try {
                    setupEdgeToEdge()
                } catch (e: Exception) {
                    // 国产机兼容性：如果 EdgeToEdge 失败，降级处理
                    handleEdgeToEdgeError(e)
                }
            }
            
            // 设置布局
            val layoutId = getLayoutId()
            if (layoutId != 0) {
                setContentView(layoutId)
            }
            
            // 初始化方法（异常保护）
            try {
                initView()
            } catch (e: Exception) {
                handleInitError("initView", e)
            }
            
            try {
                initData()
            } catch (e: Exception) {
                handleInitError("initData", e)
            }
            
            try {
                initListener()
            } catch (e: Exception) {
                handleInitError("initListener", e)
            }
            
            isCreated = true
        } catch (e: Exception) {
            // 防止 onCreate 异常导致应用崩溃
            handleCriticalError("onCreate", e)
        }
    }
    
    override fun onStart() {
        super.onStart()
        proxyManager.onStart(this)
        isVisible = true
    }
    
    override fun onResume() {
        super.onResume()
        proxyManager.onResume(this)
        isForeground = true
    }
    
    override fun onPause() {
        proxyManager.onPause(this)
        super.onPause()
        isForeground = false
    }
    
    override fun onStop() {
        proxyManager.onStop(this)
        super.onStop()
        isVisible = false
    }
    
    override fun onRestart() {
        super.onRestart()
        proxyManager.onRestart(this)
    }
    
    override fun onDestroy() {
        proxyManager.onDestroy(this)
        super.onDestroy()
        isCreated = false
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        proxyManager.onActivityResult(this, requestCode, resultCode, data)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        proxyManager.onConfigurationChanged(this, newConfig)
    }
    
    override fun onContentChanged() {
        super.onContentChanged()
        proxyManager.onContentChanged(this)
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        proxyManager.onSaveInstanceState(this, outState)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        proxyManager.onNewIntent(this, intent)
    }
    
    @Deprecated("Deprecated in Java", ReplaceWith("onBackPressedDispatcher.onBackPressed()", "androidx.activity.OnBackPressedDispatcher"))
    override fun onBackPressed() {
        if (!proxyManager.onBackPressed(this)) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
    
    @Deprecated("Deprecated in Java", ReplaceWith("onMultiWindowModeChanged(isInMultiWindowMode, newConfig)", "android.content.res.Configuration"))
    @Suppress("DEPRECATION")
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        proxyManager.onMultiWindowModeChanged(this, isInMultiWindowMode)
    }
    
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        proxyManager.onPictureInPictureModeChanged(this, isInPictureInPictureMode, newConfig)
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (proxyManager.dispatchTouchEvent(this, ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (proxyManager.onTouchEvent(this, event)) {
            return true
        }
        return super.onTouchEvent(event)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (proxyManager.onKeyDown(this, keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (proxyManager.onKeyUp(this, keyCode, event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        proxyManager.onWindowFocusChanged(this, hasFocus)
    }
    
    // ==================== EdgeToEdge 设置 ====================
    
    /**
     * 设置 EdgeToEdge 模式
     */
    private fun setupEdgeToEdge() {
        // 启用 EdgeToEdge
        enableEdgeToEdge()
        
        // 设置状态栏和导航栏文字颜色（兼容性处理）
        try {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = isLightStatusBar()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isAppearanceLightNavigationBars = isLightNavigationBar()
                }
            }
        } catch (e: Exception) {
            // 国产机兼容性：某些机型可能不支持，降级处理
            handleEdgeToEdgeError(e)
        }
        
        // 设置 WindowInsets 监听器
        setupWindowInsets()
    }
    
    /**
     * 设置 WindowInsets 监听器
     * 自动适配状态栏和导航栏
     */
    private fun setupWindowInsets() {
        try {
            val rootView = findViewById<ViewGroup>(R.id.content)?.getChildAt(0) as? ViewGroup
            rootView?.let { view ->
                ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                    try {
                        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                        
                        // 根据配置决定是否适配系统栏
                        val topPadding = if (fitStatusBar()) systemBars.top else 0
                        val bottomPadding = if (fitNavigationBar()) systemBars.bottom else 0
                        
                        v.setPadding(
                            systemBars.left,
                            topPadding,
                            systemBars.right,
                            bottomPadding
                        )
                    } catch (e: Exception) {
                        // 防止 WindowInsets 处理异常
                        handleEdgeToEdgeError(e)
                    }
                    
                    insets
                }
            }
        } catch (e: Exception) {
            // 防止 WindowInsets 设置异常
            handleEdgeToEdgeError(e)
        }
    }
    
    /**
     * 处理 EdgeToEdge 错误（降级处理）
     */
    private fun handleEdgeToEdgeError(e: Exception) {
        // 可以在这里记录日志或上报错误
        // 对于不支持 EdgeToEdge 的机型，降级为传统模式
        // 这里不做任何处理，让系统使用默认行为
    }
    
    /**
     * 处理初始化错误
     */
    private fun handleInitError(method: String, e: Exception) {
        // 可以在这里记录日志或上报错误
        // 不抛出异常，避免影响其他初始化方法
    }
    
    /**
     * 处理关键错误
     */
    private fun handleCriticalError(method: String, e: Exception) {
        // 可以在这里记录日志或上报错误
        // 对于关键错误，可以选择 finish Activity 或显示错误提示
        // 这里不做任何处理，让系统处理
    }
    
    // ==================== UI 工具方法 ====================
    
    /**
     * 显示Toast消息
     * 
     * @param message 消息内容
     * @param duration Toast显示时长，默认为 Toast.LENGTH_SHORT
     */
    protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (isCreated && !isFinishing) {
            try {
                Toast.makeText(this, message, duration).show()
            } catch (e: Exception) {
                // 防止 Toast 显示异常
            }
        }
    }
    
    /**
     * 显示Toast消息（资源ID）
     * 
     * @param resId 字符串资源ID
     * @param duration Toast显示时长，默认为 Toast.LENGTH_SHORT
     */
    protected fun showToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        try {
            showToast(getString(resId), duration)
        } catch (e: Exception) {
            // 防止资源获取异常
        }
    }
    
    /**
     * 显示加载对话框
     * 
     * 默认实现为空，子类可重写此方法来实现具体的加载对话框
     * 
     * @param message 加载提示信息，可为 null
     */
    protected open fun showLoading(message: String? = null) {
        // 子类可重写实现具体的加载对话框显示逻辑
    }
    
    /**
     * 隐藏加载对话框
     * 
     * 默认实现为空，子类可重写此方法来实现具体的加载对话框隐藏逻辑
     */
    protected open fun hideLoading() {
        // 子类可重写实现具体的加载对话框隐藏逻辑
    }
    
    // ==================== View 查找辅助方法 ====================
    
    /**
     * 通过资源ID查找View（类型安全）
     * 
     * @param id 资源ID
     * @return View实例，如果未找到则返回null
     */
    protected fun <T : View> findView(id: Int): T? {
        return try {
            findViewById<T>(id)
        } catch (e: Exception) {
            null
        }
    }
    
    // ==================== Activity 跳转辅助方法 ====================
    
    /**
     * 启动Activity
     * 
     * @param intentBuilder Intent构建器，可用于设置参数
     */
    protected inline fun <reified T : AppCompatActivity> startActivity(
        noinline intentBuilder: ((Intent) -> Unit)? = null
    ) {
        try {
            val intent = Intent(this, T::class.java)
            intentBuilder?.invoke(intent)
            startActivity(intent)
        } catch (e: Exception) {
            // 防止启动 Activity 异常
            // 可以在这里记录日志或上报错误
        }
    }
    
    /**
     * 启动Activity并等待结果
     * 
     * @param requestCode 请求码
     * @param intentBuilder Intent构建器，可用于设置参数
     * 
     * @deprecated 使用 Activity Result API 替代，参考 androidx.activity.result
     */
    @Deprecated(
        "Use Activity Result API instead",
        ReplaceWith(
            "registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> /* handle result */ }.launch(intent)",
            "androidx.activity.result.contract.ActivityResultContracts"
        )
    )
    protected inline fun <reified T : AppCompatActivity> startActivityForResult(
        requestCode: Int,
        noinline intentBuilder: ((Intent) -> Unit)? = null
    ) {
        try {
            val intent = Intent(this, T::class.java)
            intentBuilder?.invoke(intent)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            // 防止启动 Activity 异常
            // 可以在这里记录日志或上报错误
        }
    }
    
    /**
     * 结束当前Activity
     */
    protected fun finishActivity() {
        if (!isFinishing) {
            finish()
        }
    }
    
    // ==================== Activity 栈操作（便捷方法） ====================
    
    /**
     * 获取 Activity 栈管理器
     */
    protected val activityStack: ActivityStackManager
        get() = ActivityStackManager
    
    /**
     * 获取栈顶 Activity
     */
    protected fun getTopActivity(): Activity? {
        return ActivityStackManager.getTopActivity()
    }
    
    /**
     * 结束指定类型的 Activity
     */
    protected fun finishActivity(clazz: Class<out Activity>, animate: Boolean = true) {
        ActivityStackManager.finishActivity(clazz, animate)
    }
    
    /**
     * 结束除当前外的所有 Activity
     */
    protected fun finishOtherActivities(animate: Boolean = true) {
        ActivityStackManager.finishOtherActivities(this, animate)
    }
    
    /**
     * 结束到指定 Activity
     */
    protected fun finishToActivity(clazz: Class<out Activity>, includeSelf: Boolean = false, animate: Boolean = true): Boolean {
        return ActivityStackManager.finishToActivity(clazz, includeSelf, animate)
    }
    
    /**
     * 结束所有 Activity
     */
    protected fun finishAllActivities(animate: Boolean = true) {
        ActivityStackManager.finishAllActivities(animate)
    }
    
    // ==================== 其他工具方法 ====================
    
    /**
     * 设置全屏模式
     * 
     * @param fullscreen 是否全屏
     */
    protected fun setFullscreen(fullscreen: Boolean) {
        try {
            if (fullscreen) {
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                @Suppress("DEPRECATION")
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        } catch (e: Exception) {
            // 防止设置全屏异常
        }
    }
    
    /**
     * 设置屏幕常亮
     * 
     * @param keepScreenOn 是否保持屏幕常亮
     */
    protected fun setKeepScreenOn(keepScreenOn: Boolean) {
        try {
            if (keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (e: Exception) {
            // 防止设置屏幕常亮异常
        }
    }
    
    // ==================== 屏幕亮度管理 ====================
    
    /**
     * 设置当前 Activity 的屏幕亮度
     * 
     * @param brightness 亮度值（0-255），-1 表示使用系统亮度
     */
    protected fun setBrightness(@IntRange(from = -1, to = 255) brightness: Int) {
        if (brightness == -1) {
            ActivityBrightnessManager.restoreAppToSystemBrightness()
        } else {
            ActivityBrightnessManager.setActivityBrightness(brightness)
        }
    }
    
    /**
     * 获取当前屏幕亮度
     */
    protected fun getBrightness(): Int {
        return ActivityBrightnessManager.getActivityBrightness()
    }
    
    // ==================== IActivityStack 实现 ====================
    
    override fun isStrongReference(): Boolean = false
    
    override fun shouldCheckAppStatus(): Boolean = true
}
