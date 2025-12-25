package com.kernelflux.aether.ui.widget.refresh.wrapper

import android.annotation.SuppressLint
import android.view.View
import com.kernelflux.aether.ui.widget.refresh.api.RefreshFooter
import com.kernelflux.aether.ui.widget.refresh.simple.SimpleComponent

/**
 * 刷新底部包装
 * Created by scwang on 2017/5/26.
 */
@SuppressLint("ViewConstructor")
class RefreshFooterWrapper(wrapper: View) : SimpleComponent(wrapper), RefreshFooter
