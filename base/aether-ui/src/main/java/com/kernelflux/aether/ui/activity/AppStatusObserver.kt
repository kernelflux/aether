package com.kernelflux.aether.ui.activity

import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 应用状态观察者
 * 
 * 监听应用前后台状态变化
 * 使用弱引用管理监听器，避免内存泄漏
 * 
 * @author Aether Framework
 */
object AppStatusObserver {
    private val listeners = CopyOnWriteArrayList<WeakReference<AppStatusListener>>()
    
    /**
     * 注册应用状态监听器
     */
    fun register(listener: AppStatusListener) {
        listeners.add(WeakReference(listener))
        cleanUp()
    }
    
    /**
     * 注销应用状态监听器
     */
    fun unregister(listener: AppStatusListener) {
        listeners.removeAll { it.get() == listener }
        cleanUp()
    }
    
    /**
     * 通知应用进入前台
     */
    internal fun notifyForeground() {
        cleanUp()
        listeners.forEach { ref ->
            ref.get()?.onForeground()
        }
    }
    
    /**
     * 通知应用进入后台
     */
    internal fun notifyBackground() {
        cleanUp()
        listeners.forEach { ref ->
            ref.get()?.onBackground()
        }
    }
    
    /**
     * 清理无效引用
     */
    private fun cleanUp() {
        listeners.removeAll { it.get() == null }
    }
    
    /**
     * 应用状态监听器接口
     */
    interface AppStatusListener {
        /**
         * 应用进入前台
         */
        fun onForeground()
        
        /**
         * 应用进入后台
         */
        fun onBackground()
    }
}

