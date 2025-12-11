package com.kernelflux.aethersample

import android.widget.ImageView
import com.kernelflux.aether.imageloader.spi.IImageLoader
import com.kernelflux.fluxrouter.core.FluxRouter

/**
 * 图片加载服务示例页面
 *
 * @author Aether Framework
 */
class ImageLoaderActivity : BaseActivity() {

    private var imageLoader: IImageLoader? = null
    private var imageView: ImageView? = null

    override fun getContentResId(): Int = R.layout.activity_image_loader


    override fun onInitView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Image Loader"

        imageLoader = FluxRouter.getService(IImageLoader::class.java)
        imageView = findViewById(R.id.img_view)

        // 示例1: 最简单用法（自动生命周期感知和安全检查）
        imageView?.apply {
            imageLoader?.load("https://cdn.pixabay.com/photo/2024/03/30/15/51/cat-8664948_1280.jpg")
                ?.into(this)
        }

        // 示例2: 带占位符和错误图的完整配置
//         imageView?.apply {
//             imageLoader?.load("https://cdn.pixabay.com/photo/2024/03/30/15/51/cat-8664948_1280.jpg")
//                 ?.placeholder(android.R.drawable.ic_menu_gallery)
//                 ?.error(android.R.drawable.ic_dialog_alert)
//                 ?.crossFade(300)
//                 ?.into(this)
//         }

        // 示例3: 圆形图片（头像场景）
//         imageView?.apply {
//             imageLoader?.load("https://cdn.pixabay.com/photo/2024/03/30/15/51/cat-8664948_1280.jpg")
//                 ?.circle()
//                 ?.into(this)
//         }

        // 示例4: 圆角图片
//         imageView?.apply {
//             imageLoader?.load("https://cdn.pixabay.com/photo/2024/03/30/15/51/cat-8664948_1280.jpg")
//                 ?.radius(28f) // 8dp圆角
//                 ?.into(this)
//         }

        // 示例5: 使用Activity context（生命周期感知，自动安全检查）
        // imageView?.apply {
        //     imageLoader?.with(this@ImageLoaderActivity)
        //         ?.load("https://example.com/image.jpg")
        //         ?.into(this)
        // }

        // 示例6: 加载回调和请求取消
//        imageView?.apply {
//            val requestHandle =
//                imageLoader?.load("https://cdn.pixabay.com/photo/2024/03/30/15/51/cat-8664948_1280.jpg")
//                    ?.placeholder(android.R.drawable.ic_menu_gallery)
//                    ?.error(android.R.drawable.ic_dialog_alert)
//                    ?.callback(object : com.kernelflux.aether.imageloader.spi.ImageLoadCallback {
//                        override fun onStart() {}
//                        override fun onSuccess(bitmap: android.graphics.Bitmap?) {}
//                        override fun onError(throwable: Throwable) {}
//                    })
//                    ?.into(this)
//            requestHandle?.cancel()
//        }
    }

    override fun onPause() {
        super.onPause()
        imageLoader?.pauseRequests(this)
    }

    override fun onResume() {
        super.onResume()
        imageLoader?.resumeRequests(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        imageView?.let { view ->
            imageLoader?.cancelRequest(view)
        }
        imageView = null
        imageLoader = null
    }
}
