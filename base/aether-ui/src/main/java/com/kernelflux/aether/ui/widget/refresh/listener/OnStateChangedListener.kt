package com.kernelflux.aether.ui.widget.refresh.listener

import androidx.annotation.RestrictTo
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout
import com.kernelflux.aether.ui.widget.refresh.constant.RefreshState


/**
 * 刷新状态改变监听器
 * Created by scwang on 2017/5/26.
 */
interface OnStateChangedListener {
    /**
     * 【仅限框架内调用】状态改变事件 [RefreshState]
     * @param refreshLayout RefreshLayout
     * @param oldState 改变之前的状态
     * @param newState 改变之后的状态
     */
    @RestrictTo(
        RestrictTo.Scope.LIBRARY, RestrictTo.Scope.LIBRARY_GROUP, RestrictTo.Scope.SUBCLASSES
    )
    fun onStateChanged(refreshLayout: RefreshLayout, oldState: RefreshState, newState: RefreshState)
}
