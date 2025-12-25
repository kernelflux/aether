package com.kernelflux.aether.ui.widget.refresh.listener

import com.kernelflux.aether.ui.widget.refresh.api.RefreshFooter
import com.kernelflux.aether.ui.widget.refresh.api.RefreshHeader

/**
 * 多功能监听器
 * Created by scwang on 2017/5/26.
 */
interface OnMultiListener : OnRefreshLoadMoreListener, OnStateChangedListener {
    /**
     * 手指拖动下拉（会连续多次调用，添加isDragging并取代之前的onPulling、onReleasing）
     * @param header 头部
     * @param isDragging true 手指正在拖动 false 回弹动画
     * @param percent 下拉的百分比 值 = offset/footerHeight (0 - percent - (footerHeight+maxDragHeight) / footerHeight )
     * @param offset 下拉的像素偏移量  0 - offset - (footerHeight+maxDragHeight)
     * @param headerHeight 高度 HeaderHeight or FooterHeight
     * @param maxDragHeight 最大拖动高度
     */
    fun onHeaderMoving(
        header: RefreshHeader,
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        headerHeight: Int,
        maxDragHeight: Int
    )

    fun onHeaderReleased(header: RefreshHeader, headerHeight: Int, maxDragHeight: Int)
    fun onHeaderStartAnimator(header: RefreshHeader, headerHeight: Int, maxDragHeight: Int)
    fun onHeaderFinish(header: RefreshHeader, success: Boolean)

    /**
     * 手指拖动上拉（会连续多次调用，添加isDragging并取代之前的onPulling、onReleasing）
     * @param footer 尾部
     * @param isDragging true 手指正在拖动 false 回弹动画
     * @param percent 下拉的百分比 值 = offset/footerHeight (0 - percent - (footerHeight+maxDragHeight) / footerHeight )
     * @param offset 下拉的像素偏移量  0 - offset - (footerHeight+maxDragHeight)
     * @param footerHeight 高度 HeaderHeight or FooterHeight
     * @param maxDragHeight 最大拖动高度
     */
    fun onFooterMoving(
        footer: RefreshFooter,
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        footerHeight: Int,
        maxDragHeight: Int
    )

    fun onFooterReleased(footer: RefreshFooter, footerHeight: Int, maxDragHeight: Int)
    fun onFooterStartAnimator(footer: RefreshFooter, footerHeight: Int, maxDragHeight: Int)
    fun onFooterFinish(footer: RefreshFooter, success: Boolean)
}
