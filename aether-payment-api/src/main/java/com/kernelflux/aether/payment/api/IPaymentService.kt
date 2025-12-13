package com.kernelflux.aether.payment.api

import android.app.Activity

/**
 * 支付结果回调
 */
interface PaymentCallback {
    fun onSuccess(orderId: String, amount: Double)
    fun onError(error: Throwable)
    fun onCancel()
}

/**
 * 支付订单信息
 */
data class PaymentOrder(
    val orderId: String,
    val amount: Double,
    val subject: String,
    val description: String = "",
    val extraParams: Map<String, String> = emptyMap()
)

/**
 * 支付服务接口
 * 
 * @author Aether Framework
 */
interface IPaymentService {
    
    /**
     * 发起支付
     * @param activity 当前Activity
     * @param order 支付订单信息
     * @param callback 支付结果回调
     */
    fun pay(
        activity: Activity,
        order: PaymentOrder,
        callback: PaymentCallback
    )
    
    /**
     * 检查支付方式是否可用
     */
    fun isAvailable(): Boolean
    
    /**
     * 获取支付方式名称
     */
    fun getPaymentName(): String
}

