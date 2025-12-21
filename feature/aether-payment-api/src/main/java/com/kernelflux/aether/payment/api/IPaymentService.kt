package com.kernelflux.aether.payment.api

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * 支付服务接口
 */
interface IPaymentService {
    
    // ==================== 基本信息 ====================
    
    /**
     * 获取支付方式类型
     */
    fun getPaymentType(): PaymentType
    
    /**
     * 获取支付方式名称（用于UI显示）
     * 
     * @param context 上下文（可选，用于获取本地化字符串）
     */
    fun getPaymentName(context: Context? = null): String
    
    /**
     * 获取支付方式版本号（可选）
     */
    fun getVersion(): String = "1.0.0"
    
    // ==================== 环境检查 ====================
    
    /**
     * 检查支付方式是否可用
     * 
     * 包括：是否安装、是否配置、环境是否就绪等
     * 
     * @return true表示可用，false表示不可用
     */
    fun isAvailable(): Boolean
    
    /**
     * 检查支付方式是否已安装/配置
     * 
     * @return true表示已安装/配置，false表示未安装/配置
     */
    fun isInstalled(): Boolean
    
    /**
     * 检查支付环境是否就绪
     * 
     * 比isAvailable更详细，可以返回具体的未就绪原因
     * 
     * @param context 上下文（可选，用于获取本地化字符串）
     * @return 环境检查结果，包含是否就绪和原因
     */
    fun checkEnvironment(context: Context? = null): EnvironmentCheckResult {
        return EnvironmentCheckResult(
            ready = isAvailable(),
            message = if (isAvailable()) {
                com.kernelflux.aether.common.ResourceHelper.getString(
                    context,
                    PaymentResourceKeys.ENV_READY,
                    "Environment ready"
                )
            } else {
                com.kernelflux.aether.common.ResourceHelper.getString(
                    context,
                    PaymentResourceKeys.ENV_NOT_READY,
                    "Environment not ready"
                )
            }
        )
    }
    
    // ==================== 支付操作 ====================
    
    /**
     * 发起支付
     */
    fun pay(
        activity: Activity,
        order: PaymentOrder,
        callback: PaymentCallback,
        statusListener: PaymentStatusListener? = null
    )
    
    /**
     * 处理Activity结果
     */
    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean
    
    // ==================== 订单查询和验证 ====================
    
    /**
     * 查询订单状态
     * 
     * 参考当前项目的checkOrderResult实践，支持订单状态查询
     * 
     * @param orderId 订单ID
     * @param callback 查询结果回调
     */
    fun queryOrderStatus(
        orderId: String,
        callback: OrderStatusCallback,
        context: Context? = null
    ) {
        // 默认实现：不支持查询
        callback.onError(
            PaymentErrorInfo(
                code = PaymentError.SYSTEM_ERROR,
                message = com.kernelflux.aether.common.ResourceHelper.getString(
                    context,
                    PaymentResourceKeys.FEATURE_NOT_SUPPORTED_QUERY,
                    "This payment method does not support order query"
                )
            )
        )
    }
    
    /**
     * 验证订单
     * 
     * 支付成功后，应该调用服务器验证订单的真实性
     * 参考支付宝、微信的最佳实践：客户端支付成功不代表真实成功，必须服务器验证
     * 
     * @param orderId 订单ID
     * @param transactionId 第三方交易ID
     * @param callback 验证结果回调
     */
    fun verifyOrder(
        orderId: String,
        transactionId: String?,
        callback: OrderVerificationCallback,
        context: Context? = null
    ) {
        // 默认实现：不支持验证
        callback.onError(
            PaymentErrorInfo(
                code = PaymentError.SYSTEM_ERROR,
                message = com.kernelflux.aether.common.ResourceHelper.getString(
                    context,
                    PaymentResourceKeys.FEATURE_NOT_SUPPORTED_VERIFY,
                    "This payment method does not support order verification"
                )
            )
        )
    }
    
    // ==================== 生命周期管理 ====================
    
    /**
     * 初始化支付服务
     * 
     * 某些支付方式需要在应用启动时初始化（如谷歌支付需要连接BillingClient）
     * 
     * @param activity Activity上下文
     */
    fun initialize(activity: Activity) {
        // 默认实现为空，子类可重写
    }
    
    /**
     * 释放资源
     * 
     * 在不需要使用支付服务时调用，释放相关资源（如断开连接、清理回调等）
     */
    fun release() {
        // 默认实现为空，子类可重写
    }
    
    // ==================== 扩展功能 ====================
    
    /**
     * 设置支付配置
     * 
     * 用于动态调整支付行为（超时、重试等）
     * 
     * @param config 支付配置
     */
    fun setConfig(config: PaymentConfig) {
        // 默认实现为空，子类可重写
    }
    
    /**
     * 获取当前配置
     */
    fun getConfig(): PaymentConfig = PaymentConfig.DEFAULT
}

/**
 * 环境检查结果
 */
data class EnvironmentCheckResult(
    /**
     * 是否就绪
     */
    val ready: Boolean,
    
    /**
     * 检查结果消息
     */
    val message: String,
    
    /**
     * 详细信息（可选）
     */
    val details: Map<String, String> = emptyMap()
)

/**
 * 订单状态查询回调
 */
interface OrderStatusCallback {
    /**
     * 查询成功
     */
    fun onSuccess(status: OrderStatus)
    
    /**
     * 查询失败
     */
    fun onError(error: PaymentErrorInfo)
}

/**
 * 订单状态
 */
data class OrderStatus(
    /**
     * 订单ID
     */
    val orderId: String,
    
    /**
     * 支付状态
     */
    val status: PaymentStatus,
    
    /**
     * 交易ID（如果有）
     */
    val transactionId: String? = null,
    
    /**
     * 支付时间（时间戳，毫秒）
     */
    val payTime: Long? = null,
    
    /**
     * 扩展信息
     */
    val extra: Map<String, String> = emptyMap()
)

/**
 * 订单验证回调
 */
interface OrderVerificationCallback {
    /**
     * 验证成功
     */
    fun onVerified(orderId: String, verified: Boolean)
    
    /**
     * 验证失败
     */
    fun onError(error: PaymentErrorInfo)
}

