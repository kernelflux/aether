package com.kernelflux.aether.ui.widget.refresh.listener

import android.content.Context
import com.kernelflux.aether.ui.widget.refresh.api.RefreshHeader
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout

/**
 * 默认Header创建器
 * Created by scwang on 2018/1/26.
 */
interface DefaultRefreshHeaderCreator {
    fun createRefreshHeader(context: Context, layout: RefreshLayout): RefreshHeader
}
