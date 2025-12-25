package com.kernelflux.aether.ui.widget.refresh.api

import android.animation.ValueAnimator.AnimatorUpdateListener
import android.view.MotionEvent
import android.view.View
import com.kernelflux.aether.ui.widget.refresh.listener.ScrollBoundaryDecider

/**
 * 刷新内容组件
 * Created by scwang on 2017/5/26.
 */
interface RefreshContent {
    fun getView(): View
    fun getScrollableView(): View

    fun onActionDown(motionEvent: MotionEvent)

    fun setUpComponent(kernel: RefreshKernel, fixedHeader: View?, fixedFooter: View?)
    fun setScrollBoundaryDecider(boundary: ScrollBoundaryDecider)

    fun setEnableLoadMoreWhenContentNotFull(enable: Boolean)

    fun moveSpinner(spinner: Int, headerTranslationViewId: Int, footerTranslationViewId: Int)

    fun canRefresh(): Boolean
    fun canLoadMore(): Boolean

    fun scrollContentWhenFinished(spinner: Int): AnimatorUpdateListener?
}
