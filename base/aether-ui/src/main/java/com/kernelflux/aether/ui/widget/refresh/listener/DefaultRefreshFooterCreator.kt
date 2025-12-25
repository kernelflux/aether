package com.kernelflux.aether.ui.widget.refresh.listener

import android.content.Context
import com.kernelflux.aether.ui.widget.refresh.api.RefreshFooter
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout

/**
 * 默认Footer创建器
 * Created by scwang on 2018/1/26.
 */
interface DefaultRefreshFooterCreator {
    fun createRefreshFooter(context: Context, layout: RefreshLayout): RefreshFooter
}
