package com.kernelflux.aether.payment.impl.google

import android.app.Activity
import android.content.Intent
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.kernelflux.aether.payment.api.IPaymentService
import com.kernelflux.aether.payment.api.PaymentCallback
import com.kernelflux.aether.payment.api.PaymentError
import com.kernelflux.aether.payment.api.PaymentErrorInfo
import com.kernelflux.aether.payment.api.PaymentOrder
import com.kernelflux.aether.payment.api.PaymentResult
import com.kernelflux.aether.payment.api.PaymentStatusListener
import com.kernelflux.aether.payment.api.PaymentType
import android.content.Context
import com.kernelflux.aether.common.ResourceHelper
import com.kernelflux.aether.payment.api.PaymentResourceKeys
import com.kernelflux.fluxrouter.annotation.FluxService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 谷歌支付服务实现
 * 
 * 使用说明：
 * 1. 需要在Google Play Console配置商品ID
 * 2. paymentData应该包含productId（商品ID）
 * 3. 支付结果通过PurchasesUpdatedListener回调返回
 * 
 * @author Aether Framework
 */
@FluxService(interfaceClass = IPaymentService::class)
class GooglePaymentService : IPaymentService, PurchasesUpdatedListener, BillingClientStateListener {
    
    companion object {
        private const val PRODUCT_ID_KEY = "productId"
        private const val PRODUCT_TYPE_INAPP = "inapp"
        private const val PRODUCT_TYPE_SUBS = "subs"
    }
    
    private var billingClient: BillingClient? = null
    private var currentCallback: PaymentCallback? = null
    private var currentOrderId: String? = null
    private var currentProductId: String? = null
    private var currentActivity: Activity? = null
    private var isConnected = false
    private val handledPurchaseTokens = mutableSetOf<String>()
    
    override fun getPaymentType(): PaymentType = PaymentType.GOOGLE_PAY
    
    override fun getPaymentName(context: Context?): String = ResourceHelper.getString(
        context,
        PaymentResourceKeys.PAYMENT_TYPE_GOOGLE_PAY,
        "Google Pay"
    )
    
    override fun isAvailable(): Boolean {
        // 谷歌支付需要Google Play服务
        return try {
            billingClient != null && isConnected
        } catch (e: Exception) {
            false
        }
    }
    
    override fun isInstalled(): Boolean {
        // 检查Google Play服务是否可用
        return try {
            billingClient != null
        } catch (e: Exception) {
            false
        }
    }
    
    override fun initialize(activity: Activity) {
        if (billingClient != null) {
            return
        }
        
        billingClient = BillingClient.newBuilder(activity.applicationContext)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        connectBillingClient()
    }
    
    /**
     * 连接BillingClient
     */
    private fun connectBillingClient() {
        billingClient?.startConnection(this)
    }
    
    override fun pay(
        activity: Activity,
        order: PaymentOrder,
        callback: PaymentCallback,
        statusListener: PaymentStatusListener?
    ) {
        if (!isConnected) {
            callback.onFailed(
                PaymentResult.Failed(
                    orderId = order.orderId,
                    errorInfo = PaymentErrorInfo.environmentNotReady(
                        activity,
                        ResourceHelper.getString(
                            activity,
                            PaymentResourceKeys.ENV_GOOGLE_PLAY_NOT_CONNECTED,
                            "Google Play service is not connected, please try again later"
                        )
                    )
                )
            )
            // 尝试重新连接
            connectBillingClient()
            return
        }
        
        currentCallback = callback
        currentOrderId = order.orderId
        currentActivity = activity
        
        // 从paymentData或extraParams中获取productId
        val productId = extractProductId(order)
        if (productId.isNullOrEmpty()) {
            callback.onFailed(
                PaymentResult.Failed(
                    orderId = order.orderId,
                    errorInfo = PaymentErrorInfo.paymentDataEmpty(activity)
                )
            )
            return
        }
        
        currentProductId = productId
        
        // 查询商品详情
        queryProductDetails(productId, order)
    }
    
    /**
     * 从订单中提取商品ID
     */
    private fun extractProductId(order: PaymentOrder): String? {
        // 优先从paymentData中获取
        val paymentData = order.paymentData
        if (!paymentData.isNullOrEmpty()) {
            try {
                val json = JSONObject(paymentData)
                return json.optString(PRODUCT_ID_KEY)
            } catch (e: Exception) {
                // 如果不是JSON，可能是纯字符串
                if (paymentData.contains("productId")) {
                    return paymentData
                }
            }
        }
        
        // 从extraParams中获取
        return order.extraParams[PRODUCT_ID_KEY]
    }
    
    /**
     * 查询商品详情
     */
    private fun queryProductDetails(productId: String, order: PaymentOrder) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            val orderId = order.orderId
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                currentCallback?.onFailed(
                    PaymentResult.Failed(
                        orderId = orderId,
                        errorInfo = PaymentErrorInfo.queryFailed(currentActivity, billingResult.debugMessage)
                    )
                )
                clearCurrentPayment()
                return@queryProductDetailsAsync
            }
            
            if (productDetailsList.isEmpty()) {
                currentCallback?.onFailed(
                    PaymentResult.Failed(
                        orderId = orderId,
                        errorInfo = PaymentErrorInfo.productNotFound(currentActivity)
                    )
                )
                clearCurrentPayment()
                return@queryProductDetailsAsync
            }
            
            // 发起支付流程
            launchBillingFlow(productDetailsList[0], order)
        }
    }
    
    /**
     * 发起支付流程
     */
    private fun launchBillingFlow(productDetails: ProductDetails, order: PaymentOrder) {
        val orderId = order.orderId
        val activity = currentActivity ?: run {
            currentCallback?.onFailed(
                PaymentResult.Failed(
                    orderId = orderId,
                    errorInfo = PaymentErrorInfo.systemError(
                        null,
                        ResourceHelper.getString(
                            null,
                            PaymentResourceKeys.ERROR_SYSTEM_ERROR,
                            "Activity is not available"
                        )
                    )
                )
            )
            clearCurrentPayment()
            return
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .setObfuscatedAccountId(order.orderId) // 使用订单ID作为账户ID
            .build()
        
        val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
        if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
            currentCallback?.onFailed(
                PaymentResult.Failed(
                    orderId = orderId,
                    errorInfo = PaymentErrorInfo(
                        code = result?.responseCode ?: -1,
                        message = result?.debugMessage ?: "发起支付失败"
                    )
                )
            )
            clearCurrentPayment()
        }
    }
    
    /**
     * 处理购买更新
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val callback = currentCallback ?: return
        val orderId = currentOrderId ?: ""
        
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // 支付成功
                purchases?.forEach { purchase ->
                    // 避免重复处理
                    if (handledPurchaseTokens.contains(purchase.purchaseToken)) {
                        return@forEach
                    }
                    handledPurchaseTokens.add(purchase.purchaseToken)
                    
                    // 确认购买（对于非消耗型商品）
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchase(purchase)
                        
                        // 消耗购买（对于消耗型商品）
                        consumePurchase(purchase) {
                            callback.onSuccess(
                                PaymentResult.Success(
                                    orderId = orderId,
                                    transactionId = purchase.orderId,
                                    extraData = mapOf(
                                        "purchaseToken" to purchase.purchaseToken,
                                        "productId" to (purchase.products.firstOrNull() ?: "")
                                    )
                                )
                            )
                            clearCurrentPayment()
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // 用户取消
                callback.onCancelled()
                clearCurrentPayment()
            }
            else -> {
                // 支付失败
                callback.onFailed(
                    PaymentResult.Failed(
                        orderId = orderId,
                        errorInfo = PaymentErrorInfo(
                            code = billingResult.responseCode,
                            message = billingResult.debugMessage ?: "支付失败"
                        )
                    )
                )
                clearCurrentPayment()
            }
        }
    }
    
    /**
     * 确认购买（用于非消耗型商品）
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            return
        }
        
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(params) { billingResult ->
            // 确认结果不影响支付成功回调
        }
    }
    
    /**
     * 消耗购买（用于消耗型商品）
     */
    private fun consumePurchase(purchase: Purchase, onConsumed: () -> Unit) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.consumeAsync(params) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onConsumed()
            } else {
                // 消耗失败，但购买已成功，仍然回调成功
                onConsumed()
            }
        }
    }
    
    /**
     * 清理当前支付状态
     */
    private fun clearCurrentPayment() {
        currentCallback = null
        currentOrderId = null
        currentProductId = null
        currentActivity = null
    }
    
    /**
     * BillingClient连接成功
     */
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        isConnected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
        if (!isConnected) {
            // 连接失败，可以重试
            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(1000)
                connectBillingClient()
            }
        }
    }
    
    /**
     * BillingClient连接断开
     */
    override fun onBillingServiceDisconnected() {
        isConnected = false
        // 尝试重新连接
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(1000)
            connectBillingClient()
        }
    }
    
    override fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        // 谷歌支付结果通过PurchasesUpdatedListener回调，不需要处理Activity结果
        return false
    }
    
    override fun release() {
        billingClient?.endConnection()
        billingClient = null
        isConnected = false
        clearCurrentPayment()
        handledPurchaseTokens.clear()
        currentActivity = null
    }
}
