package com.kernelflux.aethersample

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.kernelflux.aether.image.spi.IImageLoader
import com.kernelflux.aether.log.spi.ILogger
import com.kernelflux.aether.login.spi.ILoginService
import com.kernelflux.aether.network.spi.INetworkClient
import com.kernelflux.aether.payment.spi.IPaymentService
import com.kernelflux.aether.share.spi.IShareService
import com.kernelflux.fluxrouter.core.FluxRouter

/**
 * Aether框架使用示例
 *
 * @author Aether Framework
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 示例1: 使用图片加载服务
        val imageView = findViewById<ImageView>(R.id.img_view)
        FluxRouter.getService(IImageLoader::class.java)?.load(
            imageView = imageView,
            url = "https://cdn.pixabay.com/photo/2024/03/30/15/51/cat-8664948_1280.jpg"
        )

        // 示例2: 使用网络服务
        FluxRouter.getService(INetworkClient::class.java)?.get(
            url = "https://cdn.pixabay.com/photo/2024/03/30/15/51/cat-8664948_1280.jpg",
            params = mapOf("id" to "123"),
            headers = emptyMap(),
            responseType = String::class.java,
            callback = object : com.kernelflux.aether.network.spi.NetworkCallback<String> {
                override fun onSuccess(data: String) {
                    // 处理成功响应
                    FluxRouter.getService(ILogger::class.java)?.d(
                        "MainActivity_tag",
                        "User data: $data"
                    )
                }

                override fun onError(error: Throwable) {
                    // 处理错误
                    FluxRouter.getService(ILogger::class.java)?.e(
                        "MainActivity_tag",
                        "Network error",
                        error
                    )
                }
            }
        )

        // 示例3: 使用登录服务
        FluxRouter.getService(ILoginService::class.java)?.login(
            activity = this,
            callback = object : com.kernelflux.aether.login.spi.LoginCallback {
                override fun onSuccess(userInfo: com.kernelflux.aether.login.spi.UserInfo) {
                    // 登录成功
                }

                override fun onError(error: Throwable) {
                    // 登录失败
                }

                override fun onCancel() {
                    // 用户取消
                }
            }
        )

        // 示例4: 使用支付服务
        FluxRouter.getService(IPaymentService::class.java)?.pay(
            activity = this,
            order = com.kernelflux.aether.payment.spi.PaymentOrder(
                orderId = "order_123",
                amount = 99.99,
                subject = "测试商品"
            ),
            callback = object : com.kernelflux.aether.payment.spi.PaymentCallback {
                override fun onSuccess(orderId: String, amount: Double) {
                    // 支付成功
                }

                override fun onError(error: Throwable) {
                    // 支付失败
                }

                override fun onCancel() {
                    // 用户取消
                }
            }
        )

        // 示例5: 使用分享服务
        FluxRouter.getService(IShareService::class.java)?.share(
            activity = this,
            content = com.kernelflux.aether.share.spi.ShareContent(
                type = com.kernelflux.aether.share.spi.ShareType.LINK,
                title = "分享标题",
                content = "分享内容",
                linkUrl = "https://example.com"
            )
        )
    }

    // 示例数据类
    data class UserResponse(
        val id: String,
        val name: String,
        val email: String
    )
}