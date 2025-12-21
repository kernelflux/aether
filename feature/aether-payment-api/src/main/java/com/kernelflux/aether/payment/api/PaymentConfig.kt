package com.kernelflux.aether.payment.api

/**
 * 支付配置
 * 
 * 用于配置支付服务的各种参数
 */
data class PaymentConfig(
    /**
     * 支付超时时间（毫秒），默认30秒
     */
    val timeoutMillis: Long = 30_000L,
    
    /**
     * 是否启用自动重试
     */
    val enableAutoRetry: Boolean = true,
    
    /**
     * 最大重试次数
     */
    val maxRetryCount: Int = 3,
    
    /**
     * 重试延迟时间（毫秒）
     */
    val retryDelayMillis: Long = 1_000L,
    
    /**
     * 是否启用支付日志
     */
    val enableLogging: Boolean = true,
    
    /**
     * 是否启用服务器验证
     * 启用后，支付成功会调用服务器验证订单
     */
    val enableServerVerification: Boolean = true,
    
    /**
     * 服务器验证URL（可选）
     * 如果为空，则使用默认的验证逻辑
     */
    val verificationUrl: String? = null,
    
    /**
     * 扩展配置
     */
    val extra: Map<String, Any> = emptyMap()
) {
    companion object {
        /**
         * 默认配置
         */
        val DEFAULT = PaymentConfig()
        
        /**
         * 快速支付配置（短超时，不重试）
         */
        val QUICK = PaymentConfig(
            timeoutMillis = 15_000L,
            enableAutoRetry = false,
            maxRetryCount = 0
        )
        
        /**
         * 稳定支付配置（长超时，多次重试）
         */
        val STABLE = PaymentConfig(
            timeoutMillis = 60_000L,
            enableAutoRetry = true,
            maxRetryCount = 5,
            retryDelayMillis = 2_000L
        )
    }
}
