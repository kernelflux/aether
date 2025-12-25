package com.kernelflux.aether.payment.impl.alipay

import android.app.Activity
import android.content.Intent
import com.alipay.sdk.app.PayTask
import com.kernelflux.aether.payment.api.IPaymentService
import com.kernelflux.aether.payment.api.PaymentCallback
import com.kernelflux.aether.payment.api.PaymentErrorInfo
import com.kernelflux.aether.payment.api.PaymentOrder
import com.kernelflux.aether.payment.api.PaymentResult
import com.kernelflux.aether.payment.api.PaymentType
import android.content.Context
import com.kernelflux.aether.payment.api.ResourceHelper
import com.kernelflux.aether.payment.api.PaymentResourceKeys
import com.kernelflux.aether.payment.api.PaymentStatusListener
import com.kernelflux.fluxrouter.annotation.FluxService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 支付宝支付服务实现
 * 
 * 使用说明：
 * 1. paymentData应该是服务器返回的支付订单信息（orderInfo）
 * 2. 支付结果通过PayResult返回，需要在Activity的onActivityResult中处理
 * 
 * @author Aether Framework
 */
@FluxService(interfaceClass = IPaymentService::class)
class AlipayPaymentService : IPaymentService {
    
    companion object {
        private const val REQUEST_CODE_PAY = 2001
        private const val RESULT_STATUS_SUCCESS = "9000"
        private const val RESULT_STATUS_PROCESSING = "8000"
        private const val RESULT_STATUS_CANCEL = "6001"
    }
    
    private var currentCallback: PaymentCallback? = null
    private var currentOrderId: String? = null
    
    override fun getPaymentType(): PaymentType = PaymentType.ALIPAY
    
    override fun getPaymentName(context: Context?): String = ResourceHelper.getString(
        context,
        PaymentResourceKeys.PAYMENT_TYPE_ALIPAY,
        "Alipay"
    )
    
    override fun isAvailable(): Boolean {
        // 支付宝支付不需要安装客户端，通过H5支付
        return true
    }
    
    override fun isInstalled(): Boolean {
        // 检查支付宝是否安装（可选，不安装也可以通过H5支付）
        // 注意：这里无法获取Context，返回true表示支持H5支付
        return true
    }
    
    override fun pay(
        activity: Activity,
        order: PaymentOrder,
        callback: PaymentCallback,
        statusListener: PaymentStatusListener?
    ) {
        currentCallback = callback
        currentOrderId = order.orderId
        
        val paymentData = order.paymentData
        if (paymentData.isNullOrEmpty()) {
            callback.onFailed(
                PaymentResult.Failed(
                    orderId = order.orderId,
                    errorInfo = PaymentErrorInfo.paymentDataEmpty(activity)
                )
            )
            return
        }
        
        // 在后台线程执行支付
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payTask = PayTask(activity)
                val result = payTask.payV2(paymentData, true)
                
                // 切换到主线程处理结果
                withContext(Dispatchers.Main) {
                    handlePayResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val orderId = currentOrderId ?: order.orderId
                    currentCallback?.onFailed(
                        PaymentResult.Failed(
                            orderId = orderId,
                            errorInfo = PaymentErrorInfo.paymentFailed(
                                activity,
                                ResourceHelper.getString(
                                    activity,
                                    PaymentResourceKeys.ERROR_PAYMENT_FAILED,
                                    "Payment exception: %s",
                                    e.message ?: ""
                                ),
                                e
                            )
                        )
                    )
                    currentCallback = null
                    currentOrderId = null
                }
            }
        }
    }
    
    /**
     * 处理支付结果
     */
    private fun handlePayResult(result: Map<String, String>) {
        val callback = currentCallback ?: return
        val orderId = currentOrderId ?: ""
        
        val resultStatus = result["resultStatus"]
        val resultInfo = result["result"] ?: ""
        
        when (resultStatus) {
            RESULT_STATUS_SUCCESS -> {
                // 支付成功
                callback.onSuccess(
                    PaymentResult.Success(
                        orderId = orderId,
                        transactionId = extractTransactionId(resultInfo),
                        extraData = result
                    )
                )
            }
            RESULT_STATUS_PROCESSING -> {
                // 支付处理中（需要查询订单状态确认）
                // 注意：这里没有Context，使用默认值
                callback.onFailed(
                    PaymentResult.Failed(
                        orderId = orderId,
                        errorInfo = PaymentErrorInfo(
                            code = 8000,
                            message = ResourceHelper.getString(
                                null,
                                PaymentResourceKeys.PAYMENT_PROCESSING,
                                "Payment processing, please check order status later"
                            )
                        )
                    )
                )
            }
            RESULT_STATUS_CANCEL -> {
                // 用户取消
                callback.onCancelled()
            }
            else -> {
                // 支付失败
                val errorMsg = result["memo"] ?: "支付失败"
                callback.onFailed(
                    PaymentResult.Failed(
                        orderId = orderId,
                        errorInfo = PaymentErrorInfo(
                            code = resultStatus?.toIntOrNull() ?: -1,
                            message = errorMsg
                        )
                    )
                )
            }
        }
        
        // 清理回调
        currentCallback = null
        currentOrderId = null
    }
    
    /**
     * 从result中提取交易号
     */
    private fun extractTransactionId(result: String): String? {
        return try {
            // result格式类似: "success=\"true\"&sign_type=\"RSA2\"&sign=\"...\"&trade_no=\"2023120122001234567890123456\"&..."
            val pairs = result.split("&")
            for (pair in pairs) {
                if (pair.startsWith("trade_no=")) {
                    return pair.substring("trade_no=".length).trim('"')
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    override fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        // 支付宝支付结果在pay方法中通过回调返回，不需要处理Activity结果
        return false
    }
    
    override fun release() {
        currentCallback = null
        currentOrderId = null
    }
}

