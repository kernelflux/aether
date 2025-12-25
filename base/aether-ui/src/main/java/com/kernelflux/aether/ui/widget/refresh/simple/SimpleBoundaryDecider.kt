package com.kernelflux.aether.ui.widget.refresh.simple

import android.graphics.PointF
import android.view.View
import com.kernelflux.aether.ui.widget.refresh.listener.ScrollBoundaryDecider
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil

/**
 * 滚动边界
 * Created by scwang on 2017/7/8.
 */
class SimpleBoundaryDecider : ScrollBoundaryDecider {
    @JvmField
    var mActionEvent: PointF? = null

    @JvmField
    var boundary: ScrollBoundaryDecider? = null

    @JvmField
    var mEnableLoadMoreWhenContentNotFull: Boolean = true


    override fun canRefresh(content: View): Boolean {
        if (boundary != null) {
            return boundary?.canRefresh(content) ?: false
        }
        //mActionEvent == null 时 canRefresh 不会动态递归搜索
        return content.let { SmartUtil.canRefresh(content, mActionEvent) }
    }

    override fun canLoadMore(content: View): Boolean {
        if (boundary != null) {
            return boundary?.canLoadMore(content) ?: false
        }
        //mActionEvent == null 时 canLoadMore 不会动态递归搜索
        return content.let {
            SmartUtil.canLoadMore(
                content,
                mActionEvent,
                mEnableLoadMoreWhenContentNotFull
            )
        }
    }

}
