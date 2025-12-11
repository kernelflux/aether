package com.kernelflux.aether.imageloader.glide

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.kernelflux.aether.imageloader.spi.ImageLoadCallback
import com.kernelflux.aether.imageloader.spi.ImagePriority
import com.kernelflux.aether.imageloader.spi.ImageRequestBuilder
import com.kernelflux.aether.imageloader.spi.ImageRequestHandle
import com.kernelflux.aether.imageloader.spi.ImageScaleType
import com.kernelflux.aether.imageloader.spi.IImageLoader
import com.kernelflux.fluxrouter.annotation.FluxService

/**
 * Glide image loader implementation
 *
 * @author kernelflux
 */
@FluxService(interfaceClass = IImageLoader::class)
class GlideImageLoader : IImageLoader {

    private var requestManager: RequestManager? = null

    override fun load(source: Any?): ImageRequestBuilder {
        return GlideImageRequestBuilder(requestManager, source)
    }

    @Suppress("DEPRECATION")
    override fun with(activity: Activity): IImageLoader {
        val loader = GlideImageLoader()
        // Check if activity is safe before creating RequestManager
        if (LifecycleSafetyUtils.isActivitySafe(activity)) {
            loader.requestManager = Glide.with(activity)
        } else {
            // Fallback to application context if activity is destroyed
            loader.requestManager = Glide.with(activity.applicationContext)
        }
        return loader
    }

    override fun with(fragment: Fragment): IImageLoader {
        val loader = GlideImageLoader()
        // Check if fragment is safe before creating RequestManager
        if (LifecycleSafetyUtils.isFragmentSafe(fragment)) {
            loader.requestManager = Glide.with(fragment)
        } else {
            // Fallback to application context if fragment is detached
            val safeContext = fragment.context?.applicationContext
            if (safeContext != null) {
                loader.requestManager = Glide.with(safeContext)
            }
        }
        return loader
    }

    override fun with(view: View): IImageLoader {
        val loader = GlideImageLoader()
        // Check if view is safe before creating RequestManager
        if (LifecycleSafetyUtils.isViewSafe(view)) {
            loader.requestManager = Glide.with(view)
        } else {
            // Fallback to application context if view is not attached
            val safeContext = view.context?.applicationContext
            if (safeContext != null) {
                loader.requestManager = Glide.with(safeContext)
            }
        }
        return loader
    }

    override fun with(context: Context): IImageLoader {
        val loader = GlideImageLoader()
        // Use safe context (will fallback to application context if needed)
        val safeContext = LifecycleSafetyUtils.getSafeContext(context)
        if (safeContext != null) {
            loader.requestManager = Glide.with(safeContext)
        }
        return loader
    }

    override fun preload(context: Context, source: Any?): ImageRequestHandle {
        val safeContext =
            LifecycleSafetyUtils.getSafeContext(context) ?: return GlideRequestHandle()
        val target = Glide.with(safeContext).load(source).preload()
        return GlideRequestHandle(target = target)
    }

    override fun clearMemoryCache(context: Context) {
        Glide.get(context).clearMemory()
    }

    override fun clearDiskCache(context: Context) {
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }

    override fun clearAllCaches(context: Context) {
        clearMemoryCache(context)
        clearDiskCache(context)
    }

    override fun pauseRequests(context: Context) {
        val safeContext = LifecycleSafetyUtils.getSafeContext(context)
        if (safeContext != null) {
            try {
                Glide.with(safeContext).pauseRequests()
            } catch (_: Exception) {
                // Silently ignore if context is no longer valid
            }
        }
    }

    override fun resumeRequests(context: Context) {
        val safeContext = LifecycleSafetyUtils.getSafeContext(context)
        if (safeContext != null) {
            try {
                Glide.with(safeContext).resumeRequests()
            } catch (_: Exception) {
                // Silently ignore if context is no longer valid
            }
        }
    }

    override fun cancelRequest(imageView: ImageView) {
        // Check if view is safe before clearing
        if (LifecycleSafetyUtils.isViewSafe(imageView)) {
            try {
                Glide.with(imageView).clear(imageView)
            } catch (_: Exception) {
                // Silently ignore if view is no longer valid
            }
        }
    }

    override fun cancelAllRequests(context: Context) {
        Glide.with(context).pauseRequests()
    }

    /**
     * Internal request handle implementation
     */
    private class GlideRequestHandle(
        private val request: Request? = null,
        private val target: com.bumptech.glide.request.target.Target<*>? = null
    ) : ImageRequestHandle {
        @Volatile
        private var cancelled = false

        @Volatile
        private var completed = false

        override fun cancel() {
            cancelled = true
            request?.clear()
            target?.let {
                try {
                    // Try to get context from target if it's a ViewTarget or CustomViewTarget
                    @Suppress("DEPRECATION")
                    when (it) {
                        is CustomViewTarget<*, *> -> {
                            Glide.with(it.view).clear(it)
                        }

                        is com.bumptech.glide.request.target.ViewTarget<*, *> -> {
                            Glide.with(it.view).clear(it)
                        }

                        else -> {
                            // For other targets, just clear the target
                            it.onLoadCleared(null)
                        }
                    }
                } catch (_: Exception) {
                    // Fallback: just mark as cancelled
                }
            }
        }

        override fun isCancelled(): Boolean = cancelled

        override fun isCompleted(): Boolean = completed

        fun markCompleted() {
            completed = true
        }
    }

    /**
     * Internal request builder implementation
     */
    private class GlideImageRequestBuilder(
        private val requestManager: RequestManager?,
        private val source: Any?
    ) : ImageRequestBuilder {

        private var placeholder: Int = 0
        private var error: Int = 0
        private var fallback: Int = 0
        private var isCircle: Boolean = false
        private var radius: Float = 0f
        private var scaleType: ImageScaleType? = null
        private var resizeWidth: Int = 0
        private var resizeHeight: Int = 0
        private var overrideWidth: Int = 0
        private var overrideHeight: Int = 0
        private var thumbnailSource: Any? = null
        private var thumbnailSize: Float = 0f
        private var crossFadeDuration: Int = -1
        private var noAnimation: Boolean = false
        private var skipMemoryCache: Boolean = false
        private var skipDiskCache: Boolean = false
        private var onlyRetrieveFromCache: Boolean = false
        private var priority: ImagePriority = ImagePriority.NORMAL
        private var callback: ImageLoadCallback? = null

        override fun placeholder(resId: Int): ImageRequestBuilder {
            this.placeholder = resId
            return this
        }

        override fun error(resId: Int): ImageRequestBuilder {
            this.error = resId
            return this
        }

        override fun fallback(resId: Int): ImageRequestBuilder {
            this.fallback = resId
            return this
        }

        override fun circle(): ImageRequestBuilder {
            this.isCircle = true
            return this
        }

        override fun radius(dp: Float): ImageRequestBuilder {
            this.radius = dp
            return this
        }

        override fun scaleType(scaleType: ImageScaleType): ImageRequestBuilder {
            this.scaleType = scaleType
            return this
        }

        override fun resize(width: Int, height: Int): ImageRequestBuilder {
            this.resizeWidth = width
            this.resizeHeight = height
            return this
        }

        override fun override(width: Int, height: Int): ImageRequestBuilder {
            this.overrideWidth = width
            this.overrideHeight = height
            return this
        }

        override fun thumbnail(thumbnailSource: Any?): ImageRequestBuilder {
            this.thumbnailSource = thumbnailSource
            return this
        }

        override fun thumbnailSize(sizeMultiplier: Float): ImageRequestBuilder {
            this.thumbnailSize = sizeMultiplier
            return this
        }

        override fun crossFade(duration: Int): ImageRequestBuilder {
            this.crossFadeDuration = duration
            return this
        }

        override fun noAnimation(): ImageRequestBuilder {
            this.noAnimation = true
            return this
        }

        override fun skipMemoryCache(): ImageRequestBuilder {
            this.skipMemoryCache = true
            return this
        }

        override fun skipDiskCache(): ImageRequestBuilder {
            this.skipDiskCache = true
            return this
        }

        override fun onlyRetrieveFromCache(): ImageRequestBuilder {
            this.onlyRetrieveFromCache = true
            return this
        }

        override fun priority(priority: ImagePriority): ImageRequestBuilder {
            this.priority = priority
            return this
        }

        override fun callback(callback: ImageLoadCallback?): ImageRequestBuilder {
            this.callback = callback
            return this
        }

        @SuppressLint("CheckResult")
        override fun into(imageView: ImageView): ImageRequestHandle {
            // Check if ImageView and its context are safe before loading
            if (!LifecycleSafetyUtils.isViewSafe(imageView)) {
                // Return a no-op handle if view is not safe
                return GlideRequestHandle()
            }

            val manager = requestManager ?: run {
                // Get safe context from ImageView
                val safeContext = LifecycleSafetyUtils.getSafeContext(imageView.context)
                if (safeContext != null) {
                    Glide.with(safeContext)
                } else {
                    // If no safe context available, return no-op handle
                    return GlideRequestHandle()
                }
            }

            val requestBuilder = manager.load(source)

            // Apply thumbnail source if provided
            if (thumbnailSource != null) {
                requestBuilder.thumbnail(manager.load(thumbnailSource))
            }

            // Build RequestOptions
            val options = RequestOptions().apply {
                // Apply thumbnail size multiplier (replaces deprecated thumbnail(float))
                if (thumbnailSize > 0f) {
                    sizeMultiplier(thumbnailSize)
                }
                if (this@GlideImageRequestBuilder.placeholder > 0) placeholder(this@GlideImageRequestBuilder.placeholder)
                if (this@GlideImageRequestBuilder.error > 0) error(this@GlideImageRequestBuilder.error)
                if (this@GlideImageRequestBuilder.fallback > 0) fallback(this@GlideImageRequestBuilder.fallback)
                if (this@GlideImageRequestBuilder.skipMemoryCache) skipMemoryCache(true)
                if (this@GlideImageRequestBuilder.skipDiskCache) diskCacheStrategy(DiskCacheStrategy.NONE)
                if (this@GlideImageRequestBuilder.onlyRetrieveFromCache) onlyRetrieveFromCache(true)

                // Resize
                if (this@GlideImageRequestBuilder.resizeWidth > 0 &&
                    this@GlideImageRequestBuilder.resizeHeight > 0
                ) {
                    override(
                        this@GlideImageRequestBuilder.resizeWidth,
                        this@GlideImageRequestBuilder.resizeHeight
                    )
                }

                // Override
                if (this@GlideImageRequestBuilder.overrideWidth > 0 &&
                    this@GlideImageRequestBuilder.overrideHeight > 0
                ) {
                    override(
                        this@GlideImageRequestBuilder.overrideWidth,
                        this@GlideImageRequestBuilder.overrideHeight
                    )
                }

                // Priority
                when (this@GlideImageRequestBuilder.priority) {
                    ImagePriority.LOW -> priority(Priority.LOW)
                    ImagePriority.NORMAL -> priority(Priority.NORMAL)
                    ImagePriority.HIGH -> priority(Priority.HIGH)
                    ImagePriority.IMMEDIATE -> priority(Priority.IMMEDIATE)
                }
            }

            // Apply transformations
            when {
                this@GlideImageRequestBuilder.isCircle -> {
                    options.transform(CircleCrop())
                }

                this@GlideImageRequestBuilder.radius > 0 -> {
                    val radiusPx = if (imageView.context != null) {
                        (this@GlideImageRequestBuilder.radius * imageView.context.resources.displayMetrics.density).toInt()
                    } else {
                        this@GlideImageRequestBuilder.radius.toInt()
                    }
                    val roundedCorners = RoundedCorners(radiusPx)

                    // Critical: When using RoundedCorners, we MUST apply a scaleType transform
                    // to override ImageView's scaleType. Otherwise ImageView's scaleType will
                    // be applied AFTER Glide's transform, causing rounded corners to be cropped.
                    // If no scaleType is explicitly set, we detect ImageView's scaleType or default to CENTER_CROP
                    val scaleTypeToUse = this@GlideImageRequestBuilder.scaleType ?: run {
                        // Detect ImageView's scaleType and convert to our enum
                        when (imageView.scaleType) {
                            ImageView.ScaleType.CENTER_CROP -> ImageScaleType.CENTER_CROP
                            ImageView.ScaleType.CENTER_INSIDE -> ImageScaleType.CENTER_INSIDE
                            ImageView.ScaleType.FIT_CENTER -> ImageScaleType.FIT_CENTER
                            ImageView.ScaleType.FIT_START -> ImageScaleType.FIT_START
                            ImageView.ScaleType.FIT_END -> ImageScaleType.FIT_END
                            ImageView.ScaleType.FIT_XY -> ImageScaleType.FIT_XY
                            else -> ImageScaleType.CENTER_CROP // Default fallback
                        }
                    }

                    when (scaleTypeToUse) {
                        ImageScaleType.CENTER_CROP -> {
                            options.transform(MultiTransformation(CenterCrop(), roundedCorners))
                        }

                        ImageScaleType.CENTER_INSIDE -> {
                            options.transform(MultiTransformation(CenterInside(), roundedCorners))
                        }

                        ImageScaleType.FIT_CENTER -> {
                            options.transform(MultiTransformation(FitCenter(), roundedCorners))
                        }

                        else -> {
                            // For other scale types, fallback to centerCrop to ensure rounded corners work
                            options.transform(MultiTransformation(CenterCrop(), roundedCorners))
                        }
                    }
                }

                else -> {
                    // No shape transform, apply scaleType if set
                    when (this@GlideImageRequestBuilder.scaleType) {
                        ImageScaleType.CENTER_CROP -> options.centerCrop()
                        ImageScaleType.CENTER_INSIDE -> options.centerInside()
                        ImageScaleType.FIT_CENTER -> options.fitCenter()
                        ImageScaleType.FIT_START -> options.fitCenter()
                        ImageScaleType.FIT_END -> options.fitCenter()
                        ImageScaleType.FIT_XY -> options.centerCrop()
                        null -> {}
                    }
                }
            }

            // Apply RequestOptions
            requestBuilder.apply(options)

            // Apply animation
            when {
                this@GlideImageRequestBuilder.noAnimation -> {
                    // No animation - don't set transition
                }

                this@GlideImageRequestBuilder.crossFadeDuration > 0 -> {
                    requestBuilder.transition(
                        DrawableTransitionOptions.with(
                            DrawableCrossFadeFactory.Builder(this@GlideImageRequestBuilder.crossFadeDuration)
                                .setCrossFadeEnabled(true)
                                .build()
                        )
                    )
                }

                else -> {
                    // Default cross fade
                    requestBuilder.transition(DrawableTransitionOptions.withCrossFade(300))
                }
            }

            // Apply callback
            val callback = this@GlideImageRequestBuilder.callback
            if (callback != null) {
                requestBuilder.listener(createRequestListener(callback))
            }

            try {
                val target = requestBuilder.into(imageView)
                return GlideRequestHandle(target = target)
            } catch (_: Exception) {
                // If loading fails due to lifecycle issues, return no-op handle
                // This prevents crashes when Activity/Fragment/View is destroyed
                return GlideRequestHandle()
            }
        }

        @SuppressLint("CheckResult")
        override fun asBitmap(callback: (Bitmap?) -> Unit): ImageRequestHandle {
            // For asBitmap, we need a context
            val manager = requestManager ?: run {
                // asBitmap requires explicit context
                callback(null) // Callback with null since we can't load
                return GlideRequestHandle()
            }
            val requestBuilder = manager.asBitmap().load(source)

            val options = RequestOptions().apply {
                if (this@GlideImageRequestBuilder.placeholder > 0) placeholder(this@GlideImageRequestBuilder.placeholder)
                if (this@GlideImageRequestBuilder.error > 0) error(this@GlideImageRequestBuilder.error)
                if (this@GlideImageRequestBuilder.fallback > 0) fallback(this@GlideImageRequestBuilder.fallback)
                if (this@GlideImageRequestBuilder.skipMemoryCache) skipMemoryCache(true)
                if (this@GlideImageRequestBuilder.skipDiskCache) diskCacheStrategy(DiskCacheStrategy.NONE)
                if (this@GlideImageRequestBuilder.onlyRetrieveFromCache) onlyRetrieveFromCache(true)

                if (this@GlideImageRequestBuilder.overrideWidth > 0 &&
                    this@GlideImageRequestBuilder.overrideHeight > 0
                ) {
                    override(
                        this@GlideImageRequestBuilder.overrideWidth,
                        this@GlideImageRequestBuilder.overrideHeight
                    )
                }

                when (this@GlideImageRequestBuilder.priority) {
                    ImagePriority.LOW -> priority(Priority.LOW)
                    ImagePriority.NORMAL -> priority(Priority.NORMAL)
                    ImagePriority.HIGH -> priority(Priority.HIGH)
                    ImagePriority.IMMEDIATE -> priority(Priority.IMMEDIATE)
                }
            }

            requestBuilder.apply(options)

            val handle = GlideRequestHandle(null)
            try {
                requestBuilder.into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        handle.markCompleted()
                        callback(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Resource cleared
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        handle.markCompleted()
                        callback(null)
                    }
                })
            } catch (_: Exception) {
                // If loading fails due to lifecycle issues, callback with null
                handle.markCompleted()
                callback(null)
            }

            return handle
        }

        @SuppressLint("CheckResult")
        override fun preload(): ImageRequestHandle {
            val manager = requestManager ?: run {
                // Preload requires explicit context, return no-op if not available
                return GlideRequestHandle()
            }
            val requestBuilder = manager.load(source)

            val options = RequestOptions().apply {
                if (this@GlideImageRequestBuilder.skipMemoryCache) skipMemoryCache(true)
                if (this@GlideImageRequestBuilder.skipDiskCache) diskCacheStrategy(DiskCacheStrategy.NONE)
                if (this@GlideImageRequestBuilder.onlyRetrieveFromCache) onlyRetrieveFromCache(true)

                if (this@GlideImageRequestBuilder.overrideWidth > 0 &&
                    this@GlideImageRequestBuilder.overrideHeight > 0
                ) {
                    override(
                        this@GlideImageRequestBuilder.overrideWidth,
                        this@GlideImageRequestBuilder.overrideHeight
                    )
                }

                when (this@GlideImageRequestBuilder.priority) {
                    ImagePriority.LOW -> priority(Priority.LOW)
                    ImagePriority.NORMAL -> priority(Priority.NORMAL)
                    ImagePriority.HIGH -> priority(Priority.HIGH)
                    ImagePriority.IMMEDIATE -> priority(Priority.IMMEDIATE)
                }
            }

            val target = requestBuilder.apply(options).preload()
            return GlideRequestHandle(target = target)
        }

        /**
         * Create RequestListener from ImageLoadCallback
         */
        private fun createRequestListener(callback: ImageLoadCallback): RequestListener<Drawable> {
            callback.onStart()
            return object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    callback.onError(e ?: Exception("Image load failed"))
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    callback.onSuccess()
                    return false
                }
            }
        }
    }
}
