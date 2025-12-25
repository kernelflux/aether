package com.kernelflux.aether.ui.widget.refresh.listener

import android.content.Context
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout

/**
 * 默认全局初始化器
 * Created by scwang on 2018/5/29 0029.
 */
interface DefaultRefreshInitializer {
    fun initialize(context: Context, layout: RefreshLayout)
}
