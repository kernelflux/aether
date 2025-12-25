package com.kernelflux.aether.ui.widget.refresh.api

import androidx.annotation.RestrictTo


/**
 * 刷新底部
 * Created by scwang on 2017/5/26.
 */
interface RefreshFooter : RefreshComponent {
    /**
     * 【仅限框架内调用】设置数据全部加载完成，将不能再次触发加载功能
     * @param noMoreData 是否有更多数据
     * @return true 支持全部加载完成的状态显示 false 不支持
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY, RestrictTo.Scope.LIBRARY_GROUP, RestrictTo.Scope.SUBCLASSES)
    fun setNoMoreData(noMoreData: Boolean): Boolean
}
