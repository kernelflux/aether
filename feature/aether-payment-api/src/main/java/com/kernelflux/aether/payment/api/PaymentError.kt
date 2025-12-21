package com.kernelflux.aether.payment.api

import android.content.Context
import com.kernelflux.aether.common.ResourceHelper

/**
 * 支付错误码
 * 
 * 参考支付宝、微信、Google Pay的错误码设计，提供统一的错误码定义
 */
object PaymentError {
    /**
     * 成功
     */
    const val SUCCESS = 0
    
    /**
     * 用户取消
     */
    const val USER_CANCEL = 1
    
    /**
     * 支付失败（通用）
     */
    const val PAYMENT_FAILED = 2
    
    /**
     * 网络错误
     */
    const val NETWORK_ERROR = 3
    
    /**
     * 参数错误
     */
    const val PARAM_ERROR = 4
    
    /**
     * 支付环境未就绪（如未安装支付应用、未连接服务等）
     */
    const val ENVIRONMENT_NOT_READY = 5
    
    /**
     * 支付超时
     */
    const val TIMEOUT = 6
    
    /**
     * 订单不存在或已处理
     */
    const val ORDER_NOT_FOUND = 7
    
    /**
     * 余额不足
     */
    const val INSUFFICIENT_BALANCE = 8
    
    /**
     * 支付渠道不可用
     */
    const val CHANNEL_UNAVAILABLE = 9
    
    /**
     * 系统错误
     */
    const val SYSTEM_ERROR = 10
    
    /**
     * 未知错误
     */
    const val UNKNOWN_ERROR = -1
}

/**
 * 支付错误信息
 */
data class PaymentErrorInfo(
    /**
     * 错误码
     */
    val code: Int,
    
    /**
     * 错误消息
     */
    val message: String,
    
    /**
     * 原始错误（如果有）
     */
    val throwable: Throwable? = null,
    
    /**
     * 是否可重试
     */
    val retryable: Boolean = false,
    
    /**
     * 扩展信息
     */
    val extra: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * 创建成功错误信息
         * 
         * @param context 上下文（可选，用于获取本地化字符串）
         */
        fun success(context: Context? = null) = PaymentErrorInfo(
            code = PaymentError.SUCCESS,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.PAYMENT_SUCCESS,
                "Payment successful"
            )
        )
        
        /**
         * 创建用户取消错误信息
         */
        fun userCancel(context: Context? = null) = PaymentErrorInfo(
            code = PaymentError.USER_CANCEL,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.PAYMENT_CANCELLED,
                "Payment cancelled"
            )
        )
        
        /**
         * 创建网络错误信息
         */
        fun networkError(context: Context? = null, throwable: Throwable? = null) = PaymentErrorInfo(
            code = PaymentError.NETWORK_ERROR,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_NETWORK_ERROR,
                "Network error, please check your connection"
            ),
            throwable = throwable,
            retryable = true
        )
        
        /**
         * 创建超时错误信息
         */
        fun timeout(context: Context? = null) = PaymentErrorInfo(
            code = PaymentError.TIMEOUT,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_TIMEOUT,
                "Payment timeout, please retry"
            ),
            retryable = true
        )
        
        /**
         * 创建环境未就绪错误信息
         */
        fun environmentNotReady(context: Context? = null, customMessage: String? = null) = PaymentErrorInfo(
            code = PaymentError.ENVIRONMENT_NOT_READY,
            message = customMessage ?: ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_ENVIRONMENT_NOT_READY,
                "Payment environment not ready"
            )
        )
        
        /**
         * 创建支付失败错误信息
         */
        fun paymentFailed(context: Context? = null, message: String? = null, throwable: Throwable? = null) = PaymentErrorInfo(
            code = PaymentError.PAYMENT_FAILED,
            message = message ?: ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_PAYMENT_FAILED,
                "Payment failed"
            ),
            throwable = throwable
        )
        
        /**
         * 创建支付数据为空错误信息
         */
        fun paymentDataEmpty(context: Context? = null) = PaymentErrorInfo(
            code = PaymentError.PARAM_ERROR,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_PAYMENT_DATA_EMPTY,
                "Payment data is empty"
            )
        )
        
        /**
         * 创建订单不存在错误信息
         */
        fun orderNotFound(context: Context? = null) = PaymentErrorInfo(
            code = PaymentError.ORDER_NOT_FOUND,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_ORDER_NOT_FOUND,
                "Order not found"
            )
        )
        
        /**
         * 创建商品不存在错误信息
         */
        fun productNotFound(context: Context? = null) = PaymentErrorInfo(
            code = PaymentError.PARAM_ERROR,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_PRODUCT_NOT_FOUND,
                "Product not found"
            )
        )
        
        /**
         * 创建查询失败错误信息
         */
        fun queryFailed(context: Context? = null, debugMessage: String? = null) = PaymentErrorInfo(
            code = PaymentError.SYSTEM_ERROR,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_QUERY_FAILED,
                "Query failed"
            ) + (debugMessage?.let { ": $it" } ?: "")
        )
        
        /**
         * 创建发起支付失败错误信息
         */
        fun launchFailed(context: Context? = null, debugMessage: String? = null) = PaymentErrorInfo(
            code = PaymentError.SYSTEM_ERROR,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_LAUNCH_FAILED,
                "Failed to launch payment"
            ) + (debugMessage?.let { ": $it" } ?: "")
        )
        
        /**
         * 创建验证失败错误信息
         */
        fun verificationFailed(context: Context? = null) = PaymentErrorInfo(
            code = PaymentError.SYSTEM_ERROR,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_VERIFICATION_FAILED,
                "Payment verification failed"
            )
        )
        
        /**
         * 创建系统错误信息
         */
        fun systemError(context: Context? = null, message: String? = null) = PaymentErrorInfo(
            code = PaymentError.SYSTEM_ERROR,
            message = message ?: ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_SYSTEM_ERROR,
                "System error"
            )
        )
        
        /**
         * 创建未知错误信息
         */
        fun unknownError(context: Context? = null) = PaymentErrorInfo(
            code = PaymentError.UNKNOWN_ERROR,
            message = ResourceHelper.getString(
                context,
                PaymentResourceKeys.ERROR_UNKNOWN,
                "Unknown error"
            )
        )
    }
}
