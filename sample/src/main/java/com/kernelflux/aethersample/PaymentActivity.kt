package com.kernelflux.aethersample

import android.content.Intent
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.aether.payment.api.IPaymentService
import com.kernelflux.aether.payment.api.PaymentCallback
import com.kernelflux.aether.payment.api.PaymentOrder
import com.kernelflux.aether.payment.api.PaymentResult
import com.kernelflux.aether.payment.api.PaymentType
import com.kernelflux.fluxrouter.core.FluxRouter
import org.json.JSONObject

/**
 * 支付服务示例页面
 * 
 * 演示如何使用支付服务接口进行支付
 * 
 * @author Aether Framework
 */
class PaymentActivity : BaseActivity() {

    // 使用日志服务，支付模块使用独立的日志文件
    private val logger: ILogger? = FluxRouter.getService(ILogger::class.java)
    
    // 支付模块专用的 logger（使用 withModule）
    private val paymentLogger: ILogger?
        get() = logger?.withModule("payment")

    private var selectedPaymentType: PaymentType = PaymentType.ALIPAY
    private val paymentServices = mutableMapOf<PaymentType, IPaymentService?>()

    override fun getContentResId(): Int = R.layout.activity_payment

    override fun onInitView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.payment_demo)
        
        // 更新标题TextView（如果layout中有）
        findViewById<TextView>(R.id.tv_title)?.text = getString(R.string.payment_demo)

        // 初始化支付服务
        initializePaymentServices()

        val statusText = findViewById<TextView>(R.id.status_text)
        val payButton = findViewById<Button>(R.id.btn_pay)
        val paymentTypeGroup = findViewById<RadioGroup>(R.id.payment_type_group)

        // 支付方式选择
        paymentTypeGroup?.setOnCheckedChangeListener { _, checkedId ->
            selectedPaymentType = when (checkedId) {
                R.id.rb_alipay -> PaymentType.ALIPAY
                R.id.rb_wechat -> PaymentType.WECHAT
                R.id.rb_google -> PaymentType.GOOGLE_PAY
                else -> PaymentType.ALIPAY
            }
            updatePaymentButton()
        }

        // 初始化支付按钮
        updatePaymentButton()

        // 支付按钮点击
        payButton.setOnClickListener {
            performPayment(statusText)
        }

        // 显示初始状态
        statusText.text = getString(R.string.payment_initial_hint)
    }

    /**
     * 初始化支付服务
     * 
     * 注意：由于 FluxRouter 只提供 getService 方法（返回单个服务），
     * 我们采用延迟获取策略：在需要时再获取对应的支付服务
     */
    private fun initializePaymentServices() {
        paymentLogger?.i("Payment", "开始初始化支付服务")
        
        // 由于 FluxRouter 只提供 getService 方法，我们采用延迟获取策略
        // 尝试获取一个服务作为示例（实际使用时会在需要时再获取）
        val service = FluxRouter.getService(IPaymentService::class.java)
        service?.let {
            val paymentType = it.getPaymentType()
            paymentServices[paymentType] = it
            it.initialize(this)
            paymentLogger?.d("Payment", "已初始化支付服务: $paymentType")
        }
        
        if (paymentServices.isEmpty()) {
            paymentLogger?.w("Payment", "未找到支付服务实现，将在使用时再尝试获取")
        } else {
            paymentLogger?.i("Payment", "支付服务初始化完成，找到 ${paymentServices.size} 个服务")
        }
    }
    
    /**
     * 获取指定类型的支付服务
     * 如果缓存中没有，则尝试从 FluxRouter 获取
     * 
     * 注意：由于 FluxRouter.getService 可能返回任意一个实现，
     * 如果返回的服务类型不匹配，则无法获取到指定类型的服务
     */
    private fun getPaymentService(paymentType: PaymentType): IPaymentService? {
        // 先从缓存获取
        paymentServices[paymentType]?.let { return it }
        
        // 缓存中没有，尝试从 FluxRouter 获取
        // 注意：由于 FluxRouter.getService 可能返回任意一个实现，
        // 我们需要检查返回的服务类型是否匹配
        val service = FluxRouter.getService(IPaymentService::class.java)
        service?.let {
            val serviceType = it.getPaymentType()
            if (serviceType == paymentType) {
                // 类型匹配，缓存并初始化
                paymentServices[paymentType] = it
                it.initialize(this)
                paymentLogger?.d("Payment", "获取到支付服务: $paymentType")
                return it
            } else {
                // 类型不匹配，缓存这个服务（可能后续会用到）
                paymentServices[serviceType] = it
                it.initialize(this)
                paymentLogger?.w("Payment", "获取到的支付服务类型不匹配: 期望 $paymentType, 实际 $serviceType")
            }
        }
        
        return null
    }

    /**
     * 更新支付按钮
     */
    private fun updatePaymentButton() {
        val payButton = findViewById<Button>(R.id.btn_pay)
        val service = getPaymentService(selectedPaymentType)
        
        if (service != null && service.isAvailable()) {
            payButton.text = getString(R.string.payment_pay_with, service.getPaymentName(this))
            payButton.isEnabled = true
        } else {
            payButton.text = getString(R.string.payment_not_available, selectedPaymentType.name)
            payButton.isEnabled = false
        }
    }

    /**
     * 执行支付
     */
    private fun performPayment(statusText: TextView) {
        paymentLogger?.i("Payment", "开始执行支付，支付方式: $selectedPaymentType")
        
        val service = getPaymentService(selectedPaymentType)
        
        if (service == null) {
            paymentLogger?.e("Payment", "支付服务未找到: $selectedPaymentType")
            statusText.text = getString(R.string.payment_service_not_found, selectedPaymentType.name)
            return
        }

        if (!service.isAvailable()) {
            paymentLogger?.w("Payment", "支付服务不可用: ${service.getPaymentName(this)}")
            statusText.text = getString(R.string.payment_service_unavailable, service.getPaymentName(this))
            return
        }

        statusText.text = getString(R.string.payment_processing)
        paymentLogger?.d("Payment", "支付服务可用，开始构建订单")

        // 构建支付订单
        val order = createPaymentOrder(selectedPaymentType)
        paymentLogger?.d("Payment", "订单创建成功，订单ID: ${order.orderId}, 金额: ${order.amount}")

        // 发起支付
        paymentLogger?.i("Payment", "发起支付请求")
        val currentOrderId = order.orderId // 保存订单ID，用于回调中引用
        service.pay(
            activity = this,
            order = order,
            callback = object : PaymentCallback {
                override fun onSuccess(result: PaymentResult.Success) {
                    paymentLogger?.i("Payment", "支付成功，订单ID: ${result.orderId}, 交易ID: ${result.transactionId}")
                    runOnUiThread {
                        val transactionInfo = result.transactionId?.let {
                            getString(R.string.payment_transaction_id, it)
                        } ?: ""
                        statusText.text = getString(R.string.payment_success, result.orderId, transactionInfo)
                    }
                }

                override fun onFailed(result: PaymentResult.Failed) {
                    paymentLogger?.e("Payment", "支付失败，订单ID: ${result.orderId}, 错误码: ${result.errorInfo.code}, 错误信息: ${result.errorInfo.message}")
                    runOnUiThread {
                        statusText.text = getString(R.string.payment_failed, result.errorInfo.code, result.errorInfo.message)
                    }
                }

                override fun onCancelled() {
                    paymentLogger?.w("Payment", "支付已取消，订单ID: $currentOrderId")
                    runOnUiThread {
                        statusText.text = getString(R.string.payment_cancelled)
                    }
                }
            }
        )
    }

    /**
     * 创建支付订单
     */
    private fun createPaymentOrder(paymentType: PaymentType): PaymentOrder {
        val orderId = "order_${System.currentTimeMillis()}"
        
        // 根据不同的支付方式构建不同的paymentData
        val paymentData = when (paymentType) {
            PaymentType.ALIPAY -> {
                // 支付宝：这里应该是服务器返回的orderInfo
                // 示例数据，实际应该从服务器获取
                """
                {
                    "orderInfo": "app_id=xxx&biz_content=xxx&charset=utf-8&method=alipay.trade.app.pay&sign_type=RSA2&timestamp=xxx&version=1.0&sign=xxx"
                }
                """.trimIndent()
            }
            PaymentType.WECHAT -> {
                // 微信支付：这里应该是服务器返回的支付参数
                // 示例数据，实际应该从服务器获取
                JSONObject().apply {
                    put("appId", "wx1234567890abcdef")
                    put("partnerId", "1234567890")
                    put("prepayId", "wx${System.currentTimeMillis()}")
                    put("packageValue", "Sign=WXPay")
                    put("nonceStr", "nonce_${System.currentTimeMillis()}")
                    put("timeStamp", "${System.currentTimeMillis() / 1000}")
                    put("sign", "signature_from_server")
                }.toString()
            }
            PaymentType.GOOGLE_PAY -> {
                // 谷歌支付：只需要productId
                JSONObject().apply {
                    put("productId", "test_product_001")
                }.toString()
            }
        }

        return PaymentOrder(
            orderId = orderId,
            amount = 99.99,
            subject = getString(R.string.payment_test_product),
            description = getString(R.string.payment_test_order),
            paymentData = paymentData,
            extraParams = mapOf(
                "paymentType" to paymentType.name
            )
        )
    }

    /**
     * 处理Activity结果
     * 某些支付方式（如微信支付、支付宝支付）会通过onActivityResult返回结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // 让所有支付服务尝试处理结果
        paymentServices.values.forEach { service ->
            service?.handleActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * 释放资源
     */
    override fun onDestroy() {
        super.onDestroy()
        paymentServices.values.forEach { service ->
            service?.release()
        }
        paymentServices.clear()
    }
}
