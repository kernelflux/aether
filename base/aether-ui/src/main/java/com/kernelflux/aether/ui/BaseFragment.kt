package com.kernelflux.aether.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * 基础Fragment
 * 
 * 提供通用的Fragment功能，所有业务Fragment应继承此类
 * 
 * @author Aether Framework
 */
abstract class BaseFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initData()
        initListener()
    }
    
    /**
     * 获取布局ID
     * 子类必须实现此方法返回布局资源ID
     */
    protected abstract fun getLayoutId(): Int
    
    /**
     * 初始化视图
     * 子类应在此方法中初始化视图组件
     */
    protected open fun initView(view: View) {
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
    
    /**
     * 显示Toast消息
     */
    protected fun showToast(message: String) {
        // 子类可重写实现具体的Toast显示逻辑
        // 这里提供默认实现，实际项目中可以使用ToastUtils等工具类
    }
    
    /**
     * 显示加载对话框
     */
    protected fun showLoading(message: String? = null) {
        // 子类可重写实现具体的加载对话框显示逻辑
    }
    
    /**
     * 隐藏加载对话框
     */
    protected fun hideLoading() {
        // 子类可重写实现具体的加载对话框隐藏逻辑
    }
}
