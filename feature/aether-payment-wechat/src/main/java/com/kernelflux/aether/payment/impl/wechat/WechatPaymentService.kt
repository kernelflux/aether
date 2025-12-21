package com.kernelflux.aether.payment.impl.wechat

import android.app.Activity
import android.content.Intent
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
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.json.JSONObject

/**
 * 微信支付服务实现
 * 
 * 使用说明：
 * 1. 需要在AndroidManifest.xml中注册WXPayEntryActivity
 * 2. 需要在Application中初始化微信API（调用initialize方法）
 * 3. 支付结果通过WXPayEntryActivity的onResp回调返回
 * 
 * @author Aether Framework
 */
@FluxService(interfaceClass = IPaymentService::class)
class WechatPaymentService : IPaymentService, IWXAPIEventHandler {
    
    companion object {
        private const val REQUEST_CODE_PAY = 1001
        private const val WECHAT_APP_ID_KEY = "appId"
        private const val WECHAT_PARTNER_ID_KEY = "partnerId"
        private const val WECHAT_PREPAY_ID_KEY = "prepayId"
        private const val WECHAT_PACKAGE_VALUE_KEY = "packageValue"
        private const val WECHAT_NONCE_STR_KEY = "nonceStr"
        private const val WECHAT_TIME_STAMP_KEY = "timeStamp"
        private const val WECHAT_SIGN_KEY = "sign"
    }
    
    private var wxApi: IWXAPI? = null
    private var currentCallback: PaymentCallback? = null
    private var currentOrderId: String? = null
    private var appId: String? = null
    
    override fun getPaymentType(): PaymentType = PaymentType.WECHAT
    
    override fun getPaymentName(context: Context?): String = ResourceHelper.getString(
        context,
        PaymentResourceKeys.PAYMENT_TYPE_WECHAT,
        "WeChat Pay"
    )
    
    override fun isAvailable(): Boolean {
        return isInstalled() && wxApi != null && wxApi!!.isWXAppInstalled
    }
    
    override fun isInstalled(): Boolean {
        return try {
            wxApi?.isWXAppInstalled == true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun initialize(activity: Activity) {
        // 从extraParams中获取appId，如果没有则使用默认值
        // 实际使用时应该从配置中读取
        appId = activity.packageName // 这里应该从配置读取真实的微信AppId
        appId?.let {
            wxApi = WXAPIFactory.createWXAPI(activity.applicationContext, it, true)
            wxApi?.registerApp(it)
        }
    }
    
    override fun pay(
        activity: Activity,
        order: PaymentOrder,
        callback: PaymentCallback,
        statusListener: PaymentStatusListener?
    ) {
        if (!isAvailable()) {
            callback.onFailed(
                PaymentResult.Failed(
                    orderId = order.orderId,
                    errorInfo = PaymentErrorInfo.environmentNotReady(
                        activity,
                        ResourceHelper.getString(
                            activity,
                            PaymentResourceKeys.ENV_WECHAT_NOT_INSTALLED,
                            "WeChat Pay is not available, please check if WeChat is installed"
                        )
                    )
                )
            )
            return
        }
        
        currentCallback = callback
        currentOrderId = order.orderId
        
        try {
            // 解析支付数据
            // paymentData应该是JSON格式，包含微信支付所需的参数
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
            
            val jsonObject = JSONObject(paymentData)
            val appId = jsonObject.optString(WECHAT_APP_ID_KEY)
                ?: order.extraParams[WECHAT_APP_ID_KEY]
                ?: this.appId
            
            if (appId.isNullOrEmpty()) {
                callback.onFailed(
                    PaymentResult.Failed(
                        orderId = order.orderId,
                        errorInfo = PaymentErrorInfo.paymentDataEmpty(activity)
                    )
                )
                return
            }
            
            // 确保API已初始化
            if (wxApi == null) {
                wxApi = WXAPIFactory.createWXAPI(activity.applicationContext, appId, true)
                wxApi?.registerApp(appId)
            }
            
            // 检查微信版本是否支持支付
            val isSupportPay = wxApi?.wxAppSupportAPI ?: 0 >= Build.PAY_SUPPORTED_SDK_INT
            if (!isSupportPay) {
                callback.onFailed(
                    PaymentResult.Failed(
                        orderId = order.orderId,
                        errorInfo = PaymentErrorInfo.environmentNotReady(
                            activity,
                            ResourceHelper.getString(
                                activity,
                                PaymentResourceKeys.ENV_WECHAT_VERSION_NOT_SUPPORTED,
                                "Current WeChat version does not support payment, please update WeChat"
                            )
                        )
                    )
                )
                return
            }
            
            // 构建支付请求
            val req = PayReq().apply {
                this.appId = appId
                partnerId = jsonObject.optString(WECHAT_PARTNER_ID_KEY)
                prepayId = jsonObject.optString(WECHAT_PREPAY_ID_KEY)
                packageValue = jsonObject.optString(WECHAT_PACKAGE_VALUE_KEY, "Sign=WXPay")
                nonceStr = jsonObject.optString(WECHAT_NONCE_STR_KEY)
                timeStamp = jsonObject.optString(WECHAT_TIME_STAMP_KEY)
                sign = jsonObject.optString(WECHAT_SIGN_KEY)
            }
            
            // 发送支付请求
            val result = wxApi?.sendReq(req) ?: false
            if (!result) {
                callback.onFailed(
                    PaymentResult.Failed(
                        orderId = order.orderId,
                        errorInfo = PaymentErrorInfo.launchFailed(activity)
                    )
                )
            }
        } catch (e: Exception) {
            callback.onFailed(
                PaymentResult.Failed(
                    orderId = order.orderId,
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
        }
    }
    
    override fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        // 微信支付结果通过WXPayEntryActivity的onResp回调处理
        // 这里返回false，实际处理在onResp方法中
        return false
    }
    
    /**
     * 处理微信支付回调
     * 此方法由WXPayEntryActivity调用
     */
    fun handleWXPayResult(resp: BaseResp) {
        val callback = currentCallback ?: return
        val orderId = currentOrderId ?: ""
        
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                // 支付成功
                callback.onSuccess(
                    PaymentResult.Success(
                        orderId = orderId,
                        transactionId = resp.transaction,
                        extraData = mapOf("errCode" to resp.errCode.toString())
                    )
                )
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                // 用户取消
                callback.onCancelled()
            }
            else -> {
                // 支付失败
                // 注意：这里没有Context，使用默认值
                val errorMsg = resp.errStr ?: ResourceHelper.getString(
                    null,
                    PaymentResourceKeys.ERROR_PAYMENT_FAILED,
                    "Payment failed"
                )
                callback.onFailed(
                    PaymentResult.Failed(
                        orderId = orderId,
                        errorInfo = PaymentErrorInfo(
                            code = resp.errCode,
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
     * IWXAPIEventHandler实现
     */
    override fun onReq(req: BaseReq?) {
        // 微信请求，支付场景不需要处理
    }
    
    override fun onResp(resp: BaseResp?) {
        // 处理微信响应
        resp?.let { handleWXPayResult(it) }
    }
    
    override fun release() {
        wxApi?.unregisterApp()
        wxApi = null
        currentCallback = null
        currentOrderId = null
    }
}
