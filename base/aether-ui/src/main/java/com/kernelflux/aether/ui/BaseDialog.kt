package com.kernelflux.aether.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.LayoutRes

/**
 * 基础Dialog
 * 
 * 提供通用的Dialog功能，所有自定义Dialog应继承此类
 * 
 * @author Aether Framework
 */
abstract class BaseDialog @JvmOverloads constructor(
    context: Context,
    themeResId: Int = 0
) : Dialog(context, themeResId) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(getLayoutId())
        initView()
        initData()
        initListener()
    }
    
    /**
     * 获取布局ID
     * 子类必须实现此方法返回布局资源ID
     */
    @LayoutRes
    protected abstract fun getLayoutId(): Int
    
    /**
     * 初始化视图
     * 子类应在此方法中初始化视图组件
     */
    protected open fun initView() {
        // 子类实现
    }
    
    /**
     * 初始化数据
     * 子类应在此方法中初始化数据
     */
    protected open fun initData() {
        // 子类实现
    }
    
    /**
     * 初始化监听器
     * 子类应在此方法中设置事件监听
     */
    protected open fun initListener() {
        // 子类实现
    }
}
