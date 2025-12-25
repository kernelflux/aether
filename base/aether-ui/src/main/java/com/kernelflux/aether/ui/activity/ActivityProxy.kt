package com.kernelflux.aether.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Activity 生命周期代理接口
 * 
 * 允许外部组件监听和干预 Activity 的生命周期
 * 用于实现功能扩展，如埋点、性能监控、权限管理等
 */
interface ActivityProxy {
    /**
     * onCreate 回调
     */
    fun onCreate(activity: BaseActivity, savedInstanceState: Bundle?) {}
    
    /**
     * onStart 回调
     */
    fun onStart(activity: BaseActivity) {}
    
    /**
     * onResume 回调
     */
    fun onResume(activity: BaseActivity) {}
    
    /**
     * onPause 回调
     */
    fun onPause(activity: BaseActivity) {}
    
    /**
     * onStop 回调
     */
    fun onStop(activity: BaseActivity) {}
    
    /**
     * onDestroy 回调
     */
    fun onDestroy(activity: BaseActivity) {}
    
    /**
     * onRestart 回调
     */
    fun onRestart(activity: BaseActivity) {}
    
    /**
     * onActivityResult 回调
     */
    fun onActivityResult(activity: BaseActivity, requestCode: Int, resultCode: Int, data: Intent?) {}
    
    /**
     * onConfigurationChanged 回调
     */
    fun onConfigurationChanged(activity: BaseActivity, newConfig: Configuration) {}
    
    /**
     * onContentChanged 回调
     */
    fun onContentChanged(activity: BaseActivity) {}
    
    /**
     * onSaveInstanceState 回调
     */
    fun onSaveInstanceState(activity: BaseActivity, outState: Bundle) {}
    
    /**
     * onNewIntent 回调
     */
    fun onNewIntent(activity: BaseActivity, intent: Intent?) {}
    
    /**
     * onBackPressed 回调
     * @return true 表示已处理，不再调用 super.onBackPressed()
     */
    fun onBackPressed(activity: BaseActivity): Boolean = false
    
    /**
     * onMultiWindowModeChanged 回调
     */
    fun onMultiWindowModeChanged(activity: BaseActivity, isInMultiWindowMode: Boolean) {}
    
    /**
     * onPictureInPictureModeChanged 回调
     */
    fun onPictureInPictureModeChanged(activity: BaseActivity, isInPictureInPictureMode: Boolean, newConfig: Configuration) {}
    
    /**
     * dispatchTouchEvent 回调
     * @return true 表示已处理，不再调用 super.dispatchTouchEvent()
     */
    fun dispatchTouchEvent(activity: BaseActivity, ev: MotionEvent?): Boolean = false
    
    /**
     * onTouchEvent 回调
     * @return true 表示已处理，不再调用 super.onTouchEvent()
     */
    fun onTouchEvent(activity: BaseActivity, event: MotionEvent?): Boolean = false
    
    /**
     * onKeyDown 回调
     * @return true 表示已处理，不再调用 super.onKeyDown()
     */
    fun onKeyDown(activity: BaseActivity, keyCode: Int, event: KeyEvent?): Boolean = false
    
    /**
     * onKeyUp 回调
     * @return true 表示已处理，不再调用 super.onKeyUp()
     */
    fun onKeyUp(activity: BaseActivity, keyCode: Int, event: KeyEvent?): Boolean = false
    
    /**
     * onWindowFocusChanged 回调
     */
    fun onWindowFocusChanged(activity: BaseActivity, hasFocus: Boolean) {}
    
    /**
     * attachBaseContext 回调
     */
    fun attachBaseContext(activity: BaseActivity, newBase: Context?) {}
}

