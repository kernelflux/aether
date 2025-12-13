package com.kernelflux.aethersample

import android.widget.Button
import android.widget.TextView
import com.kernelflux.aether.payment.api.IPaymentService
import com.kernelflux.fluxrouter.core.FluxRouter

/**
 * 支付服务示例页面
 *
 * @author Aether Framework
 */
class PaymentActivity : BaseActivity() {

    private var paymentService: IPaymentService? = null


    override fun getContentResId(): Int = R.layout.activity_payment


    override fun onInitView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Payment"

        paymentService = FluxRouter.getService(IPaymentService::class.java)

        val statusText = findViewById<TextView>(R.id.status_text)
        val payButton = findViewById<Button>(R.id.btn_pay)

        payButton.setOnClickListener {
            statusText.text = "Processing payment..."
            paymentService?.pay(
                activity = this,
                order = com.kernelflux.aether.payment.api.PaymentOrder(
                    orderId = "order_${System.currentTimeMillis()}",
                    amount = 99.99,
                    subject = "测试商品"
                ),
                callback = object : com.kernelflux.aether.payment.api.PaymentCallback {
                    override fun onSuccess(orderId: String, amount: Double) {
                        runOnUiThread {
                            statusText.text = "Payment Success!\nOrder: $orderId\nAmount: $$amount"
                        }
                    }

                    override fun onError(error: Throwable) {
                        runOnUiThread {
                            statusText.text = "Payment Error: ${error.message}"
                        }
                    }

                    override fun onCancel() {
                        runOnUiThread {
                            statusText.text = "Payment Cancelled"
                        }
                    }
                }
            )
        }
    }
}
