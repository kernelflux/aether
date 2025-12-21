package com.kernelflux.aether.payment.api

/**
 * 支付方式枚举
 */
enum class PaymentType {
    WECHAT,      // 微信支付
    ALIPAY,      // 支付宝支付
    GOOGLE_PAY   // 谷歌支付
}

/**
 * 支付结果
 * 
 * 使用 sealed class 确保类型安全，所有可能的支付结果都被明确定义
 */
sealed class PaymentResult {
    /**
     * 支付成功
     * @param orderId 订单ID
     * @param transactionId 第三方交易ID（可选）
     * @param extraData 额外数据（可选）
     * @param verified 是否已通过服务器验证（默认false，需要调用verifyOrder验证）
     */
    data class Success(
        val orderId: String,
        val transactionId: String? = null,
        val extraData: Map<String, String> = emptyMap(),
        val verified: Boolean = false
    ) : PaymentResult()

    /**
     * 支付失败
     * @param orderId 订单ID（用于追踪失败的订单）
     * @param errorInfo 错误信息
     */
    data class Failed(
        val orderId: String,
        val errorInfo: PaymentErrorInfo
    ) : PaymentResult() {
        // 兼容旧版本API
        @Deprecated("使用errorInfo代替", ReplaceWith("errorInfo.code"))
        val errorCode: Int get() = errorInfo.code
        
        @Deprecated("使用errorInfo代替", ReplaceWith("errorInfo.message"))
        val errorMessage: String get() = errorInfo.message
        
        @Deprecated("使用errorInfo代替", ReplaceWith("errorInfo.throwable"))
        val throwable: Throwable? get() = errorInfo.throwable
    }

    /**
     * 用户取消支付
     */
    object Cancelled : PaymentResult()
    
    /**
     * 支付超时
     */
    object Timeout : PaymentResult()
}

/**
 * 支付结果回调
 * 
 * 所有支付方式统一使用此回调接口处理支付结果
 * - 支持错误信息传递
 */
interface PaymentCallback {
    /**
     * 支付成功
     * 
     * 注意：支付成功不代表订单已确认，建议调用verifyOrder进行服务器验证
     */
    fun onSuccess(result: PaymentResult.Success)
    
    /**
     * 支付失败
     */
    fun onFailed(result: PaymentResult.Failed)
    
    /**
     * 用户取消支付
     */
    fun onCancelled()
    
    /**
     * 支付超时
     * 
     * 某些支付方式可能不会触发此回调，而是通过onFailed返回超时错误
     */
    fun     onTimeout() {
        // 默认实现：转换为失败回调
        // 注意：超时回调中没有订单ID，使用空字符串
        onFailed(
            PaymentResult.Failed(
                orderId = "",
                errorInfo = PaymentErrorInfo.timeout()
            )
        )
    }
}

/**
 * 支付订单信息
 * 
 * 包含发起支付所需的所有信息
 */
data class PaymentOrder(
    /**
     * 订单ID（业务订单号）
     * 必须唯一，用于防止重复支付
     */
    val orderId: String,
    
    /**
     * 支付金额（单位：元，保留两位小数）
     */
    val amount: Double,
    
    /**
     * 商品标题
     */
    val subject: String,
    
    /**
     * 商品描述（可选）
     */
    val description: String = "",
    
    /**
     * 第三方支付所需的订单信息（如charge对象、支付凭证等）
     * 不同支付方式需要的数据格式不同：
     * - 支付宝：orderInfo 字符串
     * - 微信：JSON格式的支付参数
     * - 谷歌：包含 productId 的JSON
     */
    val paymentData: String? = null,
    
    /**
     * 额外参数（可选）
     * 可用于传递支付方式特定的参数
     */
    val extraParams: Map<String, String> = emptyMap(),
    
    /**
     * 支付配置（可选）
     * 如果不提供，使用默认配置
     */
    val config: PaymentConfig? = null,
    
    /**
     * 用户ID（可选）
     * 用于支付统计和风控
     */
    val userId: String? = null,
    
    /**
     * 商品ID（可选）
     * 用于商品统计
     */
    val goodsId: String? = null
)
