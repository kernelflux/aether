package com.kernelflux.aether.payment.api

/**
 * 支付状态
 */
enum class PaymentStatus {
    /**
     * 初始化
     */
    INITIALIZED,
    
    /**
     * 支付中
     */
    PROCESSING,
    
    /**
     * 支付成功
     */
    SUCCESS,
    
    /**
     * 支付失败
     */
    FAILED,
    
    /**
     * 用户取消
     */
    CANCELLED,
    
    /**
     * 支付超时
     */
    TIMEOUT,
    
    /**
     * 待确认（某些支付方式可能需要额外确认）
     */
    PENDING_CONFIRMATION
}

/**
 * 支付状态监听器
 * 
 * 用于监听支付状态的变化，适用于需要实时了解支付进度的场景
 */
interface PaymentStatusListener {
    /**
     * 支付状态变化
     */
    fun onStatusChanged(
        orderId: String,
        status: PaymentStatus,
        info: PaymentStatusInfo?
    )
}

/**
 * 支付状态信息
 */
data class PaymentStatusInfo(
    /**
     * 当前状态
     */
    val status: PaymentStatus,
    
    /**
     * 状态描述
     */
    val message: String? = null,
    
    /**
     * 进度百分比（0-100），如果可用
     */
    val progress: Int? = null,
    
    /**
     * 扩展信息
     */
    val extra: Map<String, String> = emptyMap()
)
