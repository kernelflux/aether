package com.kernelflux.aether.network.api

/**
 * 拦截器接口
 * 用于在请求发送前和响应返回后进行拦截处理
 */
interface Interceptor {
    /**
     * 拦截请求
     * @param chain 拦截器链
     * @return 响应
     */
    fun intercept(chain: Chain): Response<*>
}

/**
 * 拦截器链
 */
interface Chain {
    /**
     * 获取请求
     */
    fun request(): Request
    
    /**
     * 继续执行下一个拦截器或发送请求
     */
    fun proceed(request: Request): Response<*>
}

/**
 * 拦截器类型
 */
enum class InterceptorType {
    /**
     * 应用层拦截器（最先执行）
     */
    APPLICATION,
    
    /**
     * 网络层拦截器（最后执行，在发送网络请求前）
     */
    NETWORK
}
