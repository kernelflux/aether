package com.kernelflux.aether.network.api

/**
 * 重试策略
 */
interface RetryStrategy {
    /**
     * 是否应该重试
     * @param request 请求
     * @param response 响应（如果存在）
     * @param exception 异常（如果存在）
     * @param retryCount 当前重试次数
     * @return 是否应该重试
     */
    fun shouldRetry(
        request: Request,
        response: Response<*>?,
        exception: Throwable?,
        retryCount: Int
    ): Boolean
    
    /**
     * 获取重试延迟时间（毫秒）
     * @param retryCount 当前重试次数
     * @return 延迟时间（毫秒）
     */
    fun getRetryDelay(retryCount: Int): Long
}

/**
 * 默认重试策略实现
 */
data class DefaultRetryStrategy(
    val maxRetries: Int = 3,
    val retryOnNetworkError: Boolean = true,
    val retryOnHttpError: Boolean = false,
    val retryableHttpCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504),
    val baseDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val backoffMultiplier: Double = 2.0
) : RetryStrategy {
    
    override fun shouldRetry(
        request: Request,
        response: Response<*>?,
        exception: Throwable?,
        retryCount: Int
    ): Boolean {
        if (retryCount >= maxRetries) {
            return false
        }
        
        // 网络异常
        if (exception != null) {
            return when (exception) {
                is NetworkException.ConnectionException -> retryOnNetworkError
                is NetworkException.TimeoutException -> retryOnNetworkError
                else -> false
            }
        }
        
        // HTTP 错误
        if (response != null && !response.isSuccessful) {
            return retryOnHttpError && response.code in retryableHttpCodes
        }
        
        return false
    }
    
    override fun getRetryDelay(retryCount: Int): Long {
        val delay = (baseDelayMs * Math.pow(backoffMultiplier, retryCount.toDouble())).toLong()
        return minOf(delay, maxDelayMs)
    }
}
