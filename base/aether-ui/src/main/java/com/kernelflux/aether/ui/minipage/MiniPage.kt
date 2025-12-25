package com.kernelflux.aether.ui.minipage

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.view.*


abstract class MiniPage(activity: Activity) : ContextWrapper(activity), KeyEvent.Callback,
    ILifeCycleProvider, IActivityHost {
    private var mHostActivity: Activity
    private var mRootView: View? = null
    var mCallbackWrapper: PageWindowCallbackWrapper? = null
    private val dispatcherState: KeyEvent.DispatcherState = KeyEvent.DispatcherState()
    var touchOffsetX: Int = 0
    var touchOffsetY: Int = 0
    var params: Pair<Intent, Any>? = null
    var mPageLifeCycleDispatcher: PageLifeCycleDispatcher? = null
    var mPageLifeCycleMonitor: IActivityHostLifeCycleMonitor? = null

    var showing: Boolean = false
    var created: Boolean = false
    var resumed: Boolean = false
    var started: Boolean = false
    var destroyed: Boolean = false
    var isDisableDismissAnim = false

    init {
        this.mPageLifeCycleDispatcher = PageLifeCycleDispatcher()
        this.mPageLifeCycleMonitor =
            object : IActivityHostLifeCycleMonitor.DefaultActivityHostLifeCycleMonitor() {
                override fun onResume() {
                    super.onResume()
                    this@MiniPage.performOnResume()
                }

                override fun onPause() {
                    super.onPause()
                    this@MiniPage.performOnPause()
                }

                override fun onStart() {
                    super.onStart()
                    this@MiniPage.performStart()
                }

                override fun onStop() {
                    super.onStop()
                    this@MiniPage.performStop()
                }

                override fun onDestroy() {
                    super.onDestroy()
                    this@MiniPage.performDestroy()
                }
            }

        this.mHostActivity = activity
        activity.window?.also {
            var callback: Window.Callback? = it.callback
            if (callback == null) {
                callback = activity
            }
            this.mCallbackWrapper = object : PageWindowCallbackWrapper(callback) {
                override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                    if (this@MiniPage.isShowing()) {
                        return this@MiniPage.dispatchKeyEvent(event)
                    }
                    return super.dispatchKeyEvent(event)
                }

                override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                    if (this@MiniPage.isShowing()) {
                        return this@MiniPage.dispatchTouchEvent(event)
                    }
                    return super.dispatchTouchEvent(event)
                }

                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    if (this@MiniPage.isShowing()) {
                        this@MiniPage.onWindowFocusChanged(hasFocus)
                    }
                    super.onWindowFocusChanged(hasFocus)
                }
            }
            this.mRootView = buildView(LayoutInflater.from(baseContext))
            performCreate()
        }
    }


    abstract fun buildView(layoutInflater: LayoutInflater): View

    fun onWindowFocusChanged(hasFocus: Boolean) {
        this.mRootView?.dispatchWindowFocusChanged(hasFocus)
    }

    fun dispatchKeyEvent(keyEvent: KeyEvent?): Boolean {
        return keyEvent?.dispatch(this, this.dispatcherState, this) ?: false;
    }

    fun dispatchTouchEvent(motionEvent: MotionEvent?): Boolean {
        return motionEvent?.let {
            if (it.action == MotionEvent.ACTION_DOWN) {
                val decorView = this.mHostActivity.window.decorView
                this.touchOffsetX = decorView.scrollX - getLeft(decorView, this.mRootView)
                this.touchOffsetY = decorView.scrollY - getTop(decorView, this.mRootView)
            }
            it.offsetLocation(this.touchOffsetX.toFloat(), this.touchOffsetY.toFloat())
            return try {
                mRootView?.dispatchTouchEvent(it) ?: false
            } catch (th: Throwable) {
                false
            }
        } ?: false
    }


    private fun reset() {
        this.resumed = false
        this.started = false
        this.destroyed = false
        this.showing = false
        this.isDisableDismissAnim = false
        this.params = null
    }

    open fun onBackPressed() {
        dismiss()
    }

    fun getLaunchParams(): Pair<Intent, Any>? {
        return this.params
    }

    fun getRootView(): View? {
        return this.mRootView
    }

    fun <T : View> findViewById(resId: Int): T? {
        return mRootView?.findViewById(resId)
    }

    fun show(params: Pair<Intent, Any>) {
        if (!isShowing()) {
            this.params = params
            handleParams()
            internalShow()
            postRunnable { performShow() }
            postRunnable { performOnResume() }
        }
    }

    private fun internalShow() {
        this.showing = true
        this.mHostActivity.window.callback = this.mCallbackWrapper
        if (this.mHostActivity is ILifeCycleProvider) {
            (this.mHostActivity as ILifeCycleProvider).registerLifeCycleMonitor(this.mPageLifeCycleMonitor)
        }
    }

    open fun dismiss() {
        internalDismiss()
        postRunnable { performDismiss() }
    }

    fun postRunnable(runnable: Runnable?): Boolean {
        return mRootView?.post(runnable) ?: false
    }

    private fun internalDismiss() {
        showing = false
        mHostActivity.window.callback = mCallbackWrapper?.getWrapped()
        dispatcherState.reset(this)
        if (mHostActivity is ILifeCycleProvider) {
            (mHostActivity as ILifeCycleProvider).unregisterLifeCycleMonitor(mPageLifeCycleMonitor)
        }
        postRunnable { performStop() }
    }

    fun isShowing(): Boolean {
        return this.showing
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return false
        }
        event?.startTracking()
        return true
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK || (event != null && (!event.isTracking || event.isCanceled))) {
            return false
        }
        onBackPressed()
        return true
    }

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun registerLifeCycleMonitor(lifeCycleMonitor: LifeCycleMonitor?) {
        this.mPageLifeCycleDispatcher?.registerLifeCycleMonitor(lifeCycleMonitor)
    }

    override fun unregisterLifeCycleMonitor(lifeCycleMonitor: LifeCycleMonitor?) {
        this.mPageLifeCycleDispatcher?.unregisterLifeCycleMonitor(lifeCycleMonitor)
    }

    override fun getHost(): Activity {
        return this.mHostActivity
    }


    private fun performCreate() {
        onCreate()
        this.mPageLifeCycleDispatcher?.dispatchOnCreate(null)
        this.created = true
    }

    private fun performShow() {
        onShow()
        this.mPageLifeCycleDispatcher?.dispatchOnShow(this)
    }


    private fun performStart() {
        onStart()
        this.mPageLifeCycleDispatcher?.dispatchOnStart()
        this.started = true
    }


    private fun performOnResume() {
        performStart()
        if (!this.resumed) {
            onResume()
            this.mPageLifeCycleDispatcher?.dispatchOnStart()
            this.resumed = true
        }
    }

    private fun performOnPause() {
        onPause()
        this.mPageLifeCycleDispatcher?.dispatchOnPause()
        this.resumed = false
    }


    private fun performStop() {
        performOnPause()
        if (this.started) {
            onStop()
            this.mPageLifeCycleDispatcher?.dispatchOnStop()
            this.started = false
        }
    }

    private fun performDismiss() {
        performDestroy()
        onDismiss()
        this.mPageLifeCycleDispatcher?.dispatchOnDismiss(this)
    }

    private fun performDestroy() {
        performOnPause()
        performStop()
        if (!this.destroyed) {
            onDestroy()
            this.mPageLifeCycleDispatcher?.dispatchOnDestroy()
            this.destroyed = true
        }
    }

    open fun onCreate() {}
    open fun onStart() {}
    open fun onResume() {}
    open fun onPause() {}
    open fun onStop() {}
    open fun onDestroy() {}

    open fun onShow() {}
    open fun onDismiss() {
        reset()
    }

    open fun handleParams() {}
}