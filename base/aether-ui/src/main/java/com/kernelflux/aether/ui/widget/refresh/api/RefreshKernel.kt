package com.kernelflux.aether.ui.widget.refresh.api

import android.animation.Animator
import android.animation.ValueAnimator
import com.kernelflux.aether.ui.widget.refresh.constant.RefreshState

/**
 * 刷新布局核心功能接口
 * 为功能复杂的 Header 或者 Footer 开放的接口
 * Created by scwang on 2017/5/26.
 */
@Suppress("unused")
interface RefreshKernel {
    fun getRefreshLayout(): RefreshLayout
    fun getRefreshContent(): RefreshContent

    fun setState(state: RefreshState): RefreshKernel

    //<editor-fold desc="视图位移 Spinner">
    /**
     * 开始执行二极刷新
     * @param open 是否展开
     * @return RefreshKernel
     */
    fun startTwoLevel(open: Boolean): RefreshKernel

    /**
     * 结束关闭二极刷新
     * @return RefreshKernel
     */
    fun finishTwoLevel(): RefreshKernel

    /**
     * 移动视图到指定位置
     * moveSpinner 的取名来自 谷歌官方的 [android.support.v4.widget.SwipeRefreshLayout]
     * @param spinner 位置 (px)
     * @param isDragging true 手指正在拖动 false 回弹动画执行
     * @return RefreshKernel
     */
    fun moveSpinner(spinner: Int, isDragging: Boolean): RefreshKernel

    /**
     * 执行动画使视图位移到指定的 位置
     * moveSpinner 的取名来自 谷歌官方的 [android.support.v4.widget.SwipeRefreshLayout]
     * @param endSpinner 指定的结束位置 (px)
     * @return ValueAnimator 如果没有执行动画 null
     */
    fun animSpinner(endSpinner: Int): ValueAnimator?

    //</editor-fold>
    //<editor-fold desc="请求事件">
    /**
     * 指定在下拉时候为 Header 或 Footer 绘制背景
     * @param internal Header Footer 调用时传 this
     * @param backgroundColor 背景颜色
     * @return RefreshKernel
     */
    fun requestDrawBackgroundFor(internal: RefreshComponent, backgroundColor: Int): RefreshKernel

    /**
     * 请求事件
     * @param internal Header Footer 调用时传 this
     * @param request 请求
     * @return RefreshKernel
     */
    fun requestNeedTouchEventFor(internal: RefreshComponent, request: Boolean): RefreshKernel

    /**
     * 请求设置默认内容滚动设置
     * @param internal Header Footer 调用时传 this
     * @param translation 移动
     * @return RefreshKernel
     */
    fun requestDefaultTranslationContentFor(
        internal: RefreshComponent,
        translation: Boolean
    ): RefreshKernel?

    /**
     * 请求重新测量 headerHeight 或 footerHeight , 要求 height 高度为 WRAP_CONTENT
     * @param internal Header Footer 调用时传 this
     * @return RefreshKernel
     */
    fun requestRemeasureHeightFor(internal: RefreshComponent): RefreshKernel

    /**
     * 设置二楼回弹时长
     * @param duration 二楼回弹时长
     * @return RefreshKernel
     */
    fun requestFloorDuration(duration: Int): RefreshKernel

    /**
     * 设置二楼底部上划关闭所占高度的比率
     * @return RefreshKernel
     */
    fun requestFloorBottomPullUpToCloseRate(rate: Float): RefreshKernel

    /**
     * 当 autoRefresh 动画结束时，处理刷新状态的事件
     * @param animation 动画对象
     * @param animationOnly 是否只播放动画，不通知事件
     * @return RefreshKernel
     */
    fun onAutoRefreshAnimationEnd(animation: Animator?, animationOnly: Boolean): RefreshKernel

    /**
     * 当 autoLoadMore 动画结束时，处理刷新状态的事件
     * @param animation 动画对象
     * @param animationOnly 是否只播放动画，不通知事件
     * @return RefreshKernel
     */
    fun onAutoLoadMoreAnimationEnd(
        animation: Animator?,
        animationOnly: Boolean
    ): RefreshKernel


}
