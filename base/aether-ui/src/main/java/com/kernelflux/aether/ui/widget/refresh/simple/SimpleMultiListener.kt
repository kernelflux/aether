package com.kernelflux.aether.ui.widget.refresh.simple

import com.kernelflux.aether.ui.widget.refresh.api.RefreshFooter
import com.kernelflux.aether.ui.widget.refresh.api.RefreshHeader
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout
import com.kernelflux.aether.ui.widget.refresh.constant.RefreshState
import com.kernelflux.aether.ui.widget.refresh.listener.OnMultiListener

/**
 * 多功能监听器
 * Created by scwang on 2017/5/26.
 */
class SimpleMultiListener : OnMultiListener {

    override fun onHeaderMoving(
        header: RefreshHeader,
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        headerHeight: Int,
        maxDragHeight: Int
    ) {
    }

    override fun onHeaderReleased(header: RefreshHeader, headerHeight: Int, maxDragHeight: Int) {
    }

    override fun onHeaderStartAnimator(
        header: RefreshHeader,
        footerHeight: Int,
        maxDragHeight: Int
    ) {
    }

    override fun onHeaderFinish(header: RefreshHeader, success: Boolean) {
    }

    override fun onFooterMoving(
        footer: RefreshFooter,
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        footerHeight: Int,
        maxDragHeight: Int
    ) {
    }

    override fun onFooterReleased(footer: RefreshFooter, footerHeight: Int, maxDragHeight: Int) {
    }

    override fun onFooterStartAnimator(
        footer: RefreshFooter,
        headerHeight: Int,
        maxDragHeight: Int
    ) {
    }

    override fun onFooterFinish(footer: RefreshFooter, success: Boolean) {
    }

    override fun onRefresh(refreshLayout: RefreshLayout) {
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
    }

    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState
    ) {
    }
}
