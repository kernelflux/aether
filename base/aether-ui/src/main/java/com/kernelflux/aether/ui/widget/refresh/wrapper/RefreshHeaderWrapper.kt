package com.kernelflux.aether.ui.widget.refresh.wrapper

import android.annotation.SuppressLint
import android.view.View
import com.kernelflux.aether.ui.widget.refresh.api.RefreshHeader
import com.kernelflux.aether.ui.widget.refresh.simple.SimpleComponent

/**
 * 刷新头部包装
 * Created by scwang on 2017/5/26.
 */
@SuppressLint("ViewConstructor")
class RefreshHeaderWrapper(wrapper: View) : SimpleComponent(wrapper), RefreshHeader
