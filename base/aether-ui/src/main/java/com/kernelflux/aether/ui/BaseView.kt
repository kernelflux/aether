package com.kernelflux.aether.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.CallSuper

/**
 * 基础View
 * 
 * 提供通用的View功能，所有自定义View应继承此类
 * 
 * @author Aether Framework
 */
abstract class BaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    init {
        initView(context, attrs, defStyleAttr)
    }
    
    /**
     * 初始化View
     * 子类应在此方法中初始化View相关设置
     */
    @CallSuper
    protected open fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        // 子类实现
    }
    
    /**
     * 获取布局ID（如果使用ViewStub等）
     * 子类可重写此方法返回布局资源ID
     */
    protected open fun getLayoutId(): Int {
        return 0
    }
}
