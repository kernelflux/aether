package com.kernelflux.aether.imageloader.spi

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment

/**
 * Image scale type
 */
enum class ImageScaleType {
    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that both
     * dimensions (width and height) of the image will be equal to or less than the
     * corresponding dimension of the view.
     */
    CENTER_INSIDE,

    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that both
     * dimensions (width and height) of the image will be equal to or larger than the
     * corresponding dimension of the view.
     */
    CENTER_CROP,

    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that both
     * dimensions (width and height) of the image will be equal to or less than the
     * corresponding dimension of the view, and the image will be centered.
     */
    FIT_CENTER,

    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that both
     * dimensions (width and height) of the image will be equal to or less than the
     * corresponding dimension of the view, and the image will be aligned to the top-left.
     */
    FIT_START,

    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that both
     * dimensions (width and height) of the image will be equal to or less than the
     * corresponding dimension of the view, and the image will be aligned to the bottom-right.
     */
    FIT_END,

    /**
     * Scale the image to fill the view, ignoring aspect ratio.
     */
    FIT_XY
}

/**
 * Image loading priority
 */
enum class ImagePriority {
    /**
     * Low priority
     */
    LOW,

    /**
     * Normal priority (default)
     */
    NORMAL,

    /**
     * High priority
     */
    HIGH,

    /**
     * Immediate priority
     */
    IMMEDIATE
}

/**
 * Image loading callback
 */
interface ImageLoadCallback {
    /**
     * Called when image loading starts
     */
    fun onStart()

    /**
     * Called when image loading succeeds
     * @param bitmap The loaded bitmap (may be null if loading into ImageView)
     */
    fun onSuccess(bitmap: Bitmap? = null)

    /**
     * Called when image loading fails
     * @param throwable The error that occurred
     */
    fun onError(throwable: Throwable)
}

/**
 * Image request handle for cancellation
 */
interface ImageRequestHandle {
    /**
     * Cancel the image request
     */
    fun cancel()

    /**
     * Check if the request is cancelled
     */
    fun isCancelled(): Boolean

    /**
     * Check if the request is completed
     */
    fun isCompleted(): Boolean
}

/**
 * Image request builder for fluent API
 */
interface ImageRequestBuilder {
    /**
     * Set placeholder drawable resource
     */
    fun placeholder(resId: Int): ImageRequestBuilder

    /**
     * Set error drawable resource
     */
    fun error(resId: Int): ImageRequestBuilder

    /**
     * Set fallback drawable resource (when source is null)
     */
    fun fallback(resId: Int): ImageRequestBuilder

    /**
     * Load as circle
     */
    fun circle(): ImageRequestBuilder

    /**
     * Set corner radius in dp
     */
    fun radius(dp: Float): ImageRequestBuilder

    /**
     * Set image scale type
     */
    fun scaleType(scaleType: ImageScaleType): ImageRequestBuilder

    /**
     * Resize image to specific dimensions (in pixels)
     */
    fun resize(width: Int, height: Int): ImageRequestBuilder

    /**
     * Override image size (in pixels)
     */
    fun override(width: Int, height: Int): ImageRequestBuilder

    /**
     * Set thumbnail source (loads a smaller version first)
     */
    fun thumbnail(thumbnailSource: Any?): ImageRequestBuilder

    /**
     * Set thumbnail size ratio (0.0 to 1.0)
     */
    fun thumbnailSize(sizeMultiplier: Float): ImageRequestBuilder

    /**
     * Enable cross fade animation
     */
    fun crossFade(duration: Int = 300): ImageRequestBuilder

    /**
     * Disable animation
     */
    fun noAnimation(): ImageRequestBuilder

    /**
     * Skip memory cache
     */
    fun skipMemoryCache(): ImageRequestBuilder

    /**
     * Skip disk cache
     */
    fun skipDiskCache(): ImageRequestBuilder

    /**
     * Only retrieve from cache (don't fetch from network)
     */
    fun onlyRetrieveFromCache(): ImageRequestBuilder

    /**
     * Set loading priority
     */
    fun priority(priority: ImagePriority): ImageRequestBuilder

    /**
     * Set loading callback
     */
    fun callback(callback: ImageLoadCallback?): ImageRequestBuilder

    /**
     * Load into ImageView
     * @return Request handle for cancellation
     */
    fun into(imageView: ImageView): ImageRequestHandle

    /**
     * Load as Bitmap
     * @param callback Callback for result
     * @return Request handle for cancellation
     */
    fun asBitmap(callback: (Bitmap?) -> Unit): ImageRequestHandle

    /**
     * Preload image
     * @return Request handle for cancellation
     */
    fun preload(): ImageRequestHandle
}

/**
 * Image loader service interface
 * 
 * Supports lifecycle-aware image loading:
 * - Activity/Fragment/View: Lifecycle-aware
 * - Context: Application-level
 * 
 * @author kernelflux
 */
interface IImageLoader {

    /**
     * Create request builder with source
     */
    fun load(source: Any?): ImageRequestBuilder

    /**
     * Load with Activity context (lifecycle-aware)
     */
    fun with(activity: Activity): IImageLoader

    /**
     * Load with Fragment context (lifecycle-aware)
     */
    fun with(fragment: Fragment): IImageLoader

    /**
     * Load with View context (lifecycle-aware, recommended for RecyclerView)
     */
    fun with(view: View): IImageLoader

    /**
     * Load with Context (not lifecycle-aware)
     */
    fun with(context: Context): IImageLoader

    /**
     * Preload image
     */
    fun preload(context: Context, source: Any?): ImageRequestHandle

    /**
     * Clear memory cache
     */
    fun clearMemoryCache(context: Context)

    /**
     * Clear disk cache
     */
    fun clearDiskCache(context: Context)

    /**
     * Clear all caches (memory + disk)
     */
    fun clearAllCaches(context: Context)

    /**
     * Pause requests (for Activity/Fragment onPause)
     */
    fun pauseRequests(context: Context)

    /**
     * Resume requests (for Activity/Fragment onResume)
     */
    fun resumeRequests(context: Context)

    /**
     * Cancel all requests for a specific ImageView
     */
    fun cancelRequest(imageView: ImageView)

    /**
     * Cancel all pending requests
     */
    fun cancelAllRequests(context: Context)
}
