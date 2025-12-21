package com.kernelflux.aether.network.api

/**
 * 拦截器接口（基础接口）
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
 * 应用层拦截器接口
 * 
 * 特点：
 * - 在重定向和重试之前执行
 * - 即使从缓存返回响应，也会执行
 * - 每个请求只执行一次（无论是否有重试或重定向）
 * - 适合：添加通用请求头、日志记录、认证、请求/响应转换
 * 
 * 执行时机：
 * Application Interceptors -> Retry & Follow-up -> Network Interceptors -> Network
 */
interface ApplicationInterceptor : Interceptor {
    /**
     * 拦截请求（应用层）
     * 
     * 注意：
     * - 即使响应来自缓存，此方法也会被调用
     * - 每个请求只调用一次，不会因为重试或重定向而重复调用
     * - 可以访问完整的请求和响应数据
     * 
     * @param chain 拦截器链
     * @return 响应
     */
    override fun intercept(chain: Chain): Response<*>
}

/**
 * 网络层拦截器接口
 * 
 * 特点：
 * - 在发送网络请求之前执行
 * - 只有真正的网络请求才会执行，缓存响应不会触发
 * - 每次网络请求都会执行（包括重试和重定向）
 * - 可以访问连接信息（IP地址、TLS配置等）
 * - 适合：网络性能监控、网络层日志、连接信息获取
 * 
 * 执行时机：
 * Application Interceptors -> Retry & Follow-up -> Network Interceptors -> Network
 */
interface NetworkInterceptor : Interceptor {
    /**
     * 拦截请求（网络层）
     * 
     * 注意：
     * - 只有真正的网络请求才会调用此方法，缓存响应不会触发
     * - 每次网络请求都会调用（包括重试和重定向）
     * - 可以访问底层连接信息（如果实现支持）
     * 
     * @param chain 拦截器链
     * @return 响应
     */
    override fun intercept(chain: Chain): Response<*>
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