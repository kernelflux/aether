package com.kernelflux.aether.ui.minipage

import android.os.Build
import android.view.*
import android.view.accessibility.AccessibilityEvent


open class PageWindowCallbackWrapper(callback: Window.Callback?) : Window.Callback {
    private var mOriginalWindowCallback: Window.Callback? = null

    init {
        if (callback != null) {
            this.mOriginalWindowCallback = callback
        } else {
            throw IllegalArgumentException("Window callback my not be null !")
        }
    }

    fun getWrapped(): Window.Callback? {
        return this.mOriginalWindowCallback
    }


    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return this.mOriginalWindowCallback?.dispatchKeyEvent(event) ?: false
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
        return this.mOriginalWindowCallback?.dispatchKeyShortcutEvent(event) ?: false
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return this.mOriginalWindowCallback?.dispatchTouchEvent(event) ?: false
    }

    override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
        return this.mOriginalWindowCallback?.dispatchTrackballEvent(event) ?: false
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        return this.mOriginalWindowCallback?.dispatchGenericMotionEvent(event) ?: false
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
        return this.mOriginalWindowCallback?.dispatchPopulateAccessibilityEvent(event) ?: false
    }

    override fun onCreatePanelView(featureId: Int): View? {
        return this.mOriginalWindowCallback?.onCreatePanelView(featureId)
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        return this.mOriginalWindowCallback?.onCreatePanelMenu(featureId, menu) ?: false
    }

    override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean {
        return this.mOriginalWindowCallback?.onPreparePanel(featureId, view, menu) ?: false
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return this.mOriginalWindowCallback?.onMenuOpened(featureId, menu) ?: false
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        return this.mOriginalWindowCallback?.onMenuItemSelected(featureId, item) ?: false
    }

    override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
        this.mOriginalWindowCallback?.onWindowAttributesChanged(attrs)
    }

    override fun onContentChanged() {
        this.mOriginalWindowCallback?.onContentChanged()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        this.mOriginalWindowCallback?.onWindowFocusChanged(hasFocus)
    }

    override fun onAttachedToWindow() {
        this.mOriginalWindowCallback?.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        this.mOriginalWindowCallback?.onDetachedFromWindow()
    }

    override fun onPanelClosed(featureId: Int, menu: Menu) {
        this.mOriginalWindowCallback?.onPanelClosed(featureId, menu)
    }

    override fun onSearchRequested(): Boolean {
        return this.mOriginalWindowCallback?.onSearchRequested() ?: false
    }

    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return this.mOriginalWindowCallback?.onSearchRequested(searchEvent) ?: false
        }
        return false
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return this.mOriginalWindowCallback?.onWindowStartingActionMode(callback)
    }

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int
    ): ActionMode? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return this.mOriginalWindowCallback?.onWindowStartingActionMode(callback, type)
        }
        return null
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        this.mOriginalWindowCallback?.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        this.mOriginalWindowCallback?.onActionModeFinished(mode)
    }
}