package com.kernelflux.aether.image.impl.glide

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.kernelflux.aether.image.spi.IImageLoader
import com.kernelflux.fluxrouter.annotation.FluxService

/**
 * Glide图片加载实现
 *
 * @author Aether Framework
 */
@FluxService(interfaceClass = IImageLoader::class)
class GlideImageLoader : IImageLoader {

    private lateinit var context: Context

    override fun load(
        imageView: ImageView,
        url: String?,
        placeholder: Int,
        error: Int
    ) {
        val requestOptions = RequestOptions().apply {
            if (placeholder > 0) placeholder(placeholder)
            if (error > 0) error(error)
        }

        Glide.with(imageView)
            .load(url)
            .apply(requestOptions)
            .into(imageView)
    }

    override fun loadCircle(
        imageView: ImageView,
        url: String?,
        placeholder: Int,
        error: Int
    ) {
        val requestOptions = RequestOptions()
            .transform(CircleCrop())
            .apply {
                if (placeholder > 0) placeholder(placeholder)
                if (error > 0) error(error)
            }

        Glide.with(imageView)
            .load(url)
            .apply(requestOptions)
            .into(imageView)
    }

    override fun loadRound(
        imageView: ImageView,
        url: String?,
        radius: Float,
        placeholder: Int,
        error: Int
    ) {
        val requestOptions = RequestOptions()
            .transform(RoundedCorners((radius * imageView.context.resources.displayMetrics.density).toInt()))
            .apply {
                if (placeholder > 0) placeholder(placeholder)
                if (error > 0) error(error)
            }

        Glide.with(imageView)
            .load(url)
            .apply(requestOptions)
            .into(imageView)
    }

    override fun clearMemoryCache() {
        if (::context.isInitialized) {
            Glide.get(context).clearMemory()
        }
    }

    override fun clearDiskCache() {
        if (::context.isInitialized) {
            Thread {
                Glide.get(context).clearDiskCache()
            }.start()
        }
    }
}

