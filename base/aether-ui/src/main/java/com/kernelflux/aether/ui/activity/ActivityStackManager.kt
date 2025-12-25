package com.kernelflux.aether.ui.activity

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Activity 栈管理器
 *
 * 使用 Application.ActivityLifecycleCallbacks 实现
 * 支持强引用和弱引用混合管理，避免内存泄漏
 */
object ActivityStackManager : Application.ActivityLifecycleCallbacks {

    // 使用 CopyOnWriteArrayList 保证线程安全
    private val activityStack = CopyOnWriteArrayList<WeakReference<Activity>>()
    private val strongRefStack = CopyOnWriteArrayList<Activity>()

    // 应用前后台状态
    private var foregroundCount = 0
    private var configCount = 0
    private var isBackground = false

    // 监听器
    private val stackListeners = CopyOnWriteArrayList<ActivityStackListener>()

    /**
     * 初始化（在 Application.onCreate 中调用）
     */
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * 反初始化
     */
    fun unInit(application: Application) {
        application.unregisterActivityLifecycleCallbacks(this)
        activityStack.clear()
        strongRefStack.clear()
        stackListeners.clear()
    }

    /**
     * 注册栈变化监听器
     */
    fun registerStackListener(listener: ActivityStackListener) {
        stackListeners.add(listener)
    }

    /**
     * 注销栈变化监听器
     */
    fun unregisterStackListener(listener: ActivityStackListener) {
        stackListeners.remove(listener)
    }


    /**
     * 获取 Activity 栈（过滤已销毁的 Activity）
     */
    fun getActivityStack(): List<Activity> {
        val result = mutableListOf<Activity>()

        // 先添加强引用
        strongRefStack.forEach { activity ->
            if (isActivityAlive(activity)) {
                result.add(activity)
            }
        }

        // 再添加弱引用（去重）
        activityStack.forEach { ref ->
            ref.get()?.let { activity ->
                if (isActivityAlive(activity) && !result.contains(activity)) {
                    result.add(activity)
                }
            }
        }

        // 清理无效引用
        activityStack.removeAll { it.get() == null }

        return result
    }

    /**
     * 获取栈顶 Activity
     */
    fun getTopActivity(): Activity? {
        val stack = getActivityStack()
        return stack.lastOrNull()
    }

    /**
     * 获取栈大小
     */
    fun size(): Int = getActivityStack().size

    /**
     * 判断栈是否为空
     */
    fun isEmpty(): Boolean = getActivityStack().isEmpty()

    /**
     * 判断是否包含指定 Activity
     */
    fun contains(activity: Activity): Boolean {
        return getActivityStack().contains(activity)
    }

    /**
     * 判断是否包含指定类型的 Activity
     */
    fun contains(clazz: Class<out Activity>): Boolean {
        return getActivityStack().any { it.javaClass == clazz }
    }

    /**
     * 获取指定类型的 Activity
     */
    fun getActivity(clazz: Class<out Activity>): Activity? {
        return getActivityStack().find { it.javaClass == clazz }
    }

    /**
     * 结束指定 Activity
     */
    fun finishActivity(activity: Activity, animate: Boolean = true) {
        if (isActivityAlive(activity)) {
            activity.finish()
            if (!animate) {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    /**
     * 结束指定类型的 Activity
     */
    fun finishActivity(clazz: Class<out Activity>, animate: Boolean = true) {
        getActivityStack().forEach { activity ->
            if (activity.javaClass == clazz) {
                finishActivity(activity, animate)
            }
        }
    }

    /**
     * 结束除指定 Activity 外的所有 Activity
     */
    fun finishOtherActivities(activity: Activity, animate: Boolean = true) {
        getActivityStack().forEach { act ->
            if (act != activity) {
                finishActivity(act, animate)
            }
        }
    }

    /**
     * 结束除指定类型外的所有 Activity
     */
    fun finishOtherActivities(clazz: Class<out Activity>, animate: Boolean = true) {
        getActivityStack().forEach { activity ->
            if (activity.javaClass != clazz) {
                finishActivity(activity, animate)
            }
        }
    }

    /**
     * 结束所有 Activity
     */
    fun finishAllActivities(animate: Boolean = true) {
        getActivityStack().forEach { activity ->
            finishActivity(activity, animate)
        }
    }

    /**
     * 结束到指定 Activity（不包括该 Activity）
     */
    fun finishToActivity(
        clazz: Class<out Activity>,
        includeSelf: Boolean = false,
        animate: Boolean = true
    ): Boolean {
        if (!contains(clazz)) {
            return false
        }

        val stack = getActivityStack()
        var found = false

        stack.forEach { activity ->
            if (activity.javaClass == clazz) {
                found = true
                if (includeSelf) {
                    finishActivity(activity, animate)
                }
            } else if (!found) {
                finishActivity(activity, animate)
            }
        }

        return found
    }

    /**
     * 判断应用是否在后台
     */
    fun isBackground(): Boolean = isBackground

    /**
     * 判断 Activity 是否存活
     */
    private fun isActivityAlive(activity: Activity?): Boolean {
        if (activity == null || activity.isFinishing) {
            return false
        }
        // 兼容低版本 Android
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                !activity.isDestroyed
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    /**
     * 将 Activity 添加到栈顶
     */
    private fun addToStack(activity: Activity) {
        // 检查是否需要强引用（BaseActivity 可以标记）
        val needStrong =
            activity is IActivityStack && (activity as IActivityStack).isStrongReference()

        if (needStrong && !strongRefStack.contains(activity)) {
            strongRefStack.add(activity)
        }

        // 移除旧的弱引用
        activityStack.removeAll { it.get() == activity }

        // 添加到弱引用栈
        activityStack.add(WeakReference(activity))

        // 通知监听器
        stackListeners.forEach { it.onActivityAdded(activity) }
    }

    /**
     * 从栈中移除 Activity
     */
    private fun removeFromStack(activity: Activity) {
        strongRefStack.remove(activity)
        activityStack.removeAll { it.get() == activity }

        // 通知监听器
        stackListeners.forEach { it.onActivityRemoved(activity) }
    }

    // ==================== Application.ActivityLifecycleCallbacks ====================

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        addToStack(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is IActivityStack && !(activity as IActivityStack).shouldCheckAppStatus()) {
            return
        }

        if (!isBackground) {
            addToStack(activity)
        }

        if (configCount < 0) {
            configCount++
        } else {
            foregroundCount++
        }
    }

    override fun onActivityResumed(activity: Activity) {
        addToStack(activity)

        if (isBackground && activity is IActivityStack && (activity as IActivityStack).shouldCheckAppStatus()) {
            isBackground = false
            AppStatusObserver.notifyForeground()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // 不做处理
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity is IActivityStack && !(activity as IActivityStack).shouldCheckAppStatus()) {
            return
        }

        if (activity.isChangingConfigurations) {
            configCount--
            return
        }

        foregroundCount--
        if (foregroundCount <= 0) {
            isBackground = true
            AppStatusObserver.notifyBackground()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // 不做处理
    }

    override fun onActivityDestroyed(activity: Activity) {
        removeFromStack(activity)
    }

    // ==================== 接口定义 ====================

    /**
     * Activity 栈变化监听器
     */
    interface ActivityStackListener {
        fun onActivityAdded(activity: Activity)
        fun onActivityRemoved(activity: Activity)
    }

}

/**
 * Activity 栈接口
 * Activity 可以实现此接口来自定义栈行为
 */
interface IActivityStack {
    /**
     * 是否使用强引用（默认 false，使用弱引用）
     * 强引用可以防止 Activity 被 GC，但可能导致内存泄漏
     * 建议只在特殊场景使用（如需要长时间保持 Activity 引用）
     */
    fun isStrongReference(): Boolean = false

    /**
     * 是否检查应用状态（默认 true）
     * 如果返回 false，该 Activity 的生命周期变化不会影响应用前后台状态判断
     */
    fun shouldCheckAppStatus(): Boolean = true
}

