package com.kernelflux.aether.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 基础Activity
 * 
 * 提供通用的Activity功能，所有业务Activity应继承此类
 * 
 * @author Aether Framework
 */
abstract class BaseActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
        initListener()
    }
    
    /**
     * 初始化视图
     * 子类应在此方法中设置布局
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
