package com.kernelflux.aether.ui.widget.refresh.classics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.kernelflux.aether.ui.widget.refresh.api.RefreshComponent
import com.kernelflux.aether.ui.widget.refresh.api.RefreshKernel
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout
import com.kernelflux.aether.ui.widget.refresh.constant.SpinnerStyle
import com.kernelflux.aether.ui.widget.refresh.drawable.PaintDrawable
import com.kernelflux.aether.ui.widget.refresh.simple.SimpleComponent
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil
import androidx.core.view.size
import com.kernelflux.aether.ui.R

@Suppress("unused")
abstract class ClassicsAbstract<T : ClassicsAbstract<T>>(
    context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : SimpleComponent(context, attrs, defStyleAttr), RefreshComponent {
    @JvmField
    protected var mTitleText: TextView? = null

    @JvmField
    protected var mArrowView: ImageView? = null

    @JvmField
    protected var mProgressView: ImageView? = null

    @JvmField
    protected var mRefreshKernel: RefreshKernel? = null

    @JvmField
    protected var mArrowDrawable: PaintDrawable? = null

    @JvmField
    protected var mProgressDrawable: PaintDrawable? = null

    protected var mSetAccentColor: Boolean = false
    protected var mSetPrimaryColor: Boolean = false
    protected var mBackgroundColor: Int = 0

    @JvmField
    protected var mFinishDuration: Int = 500
    protected var mPaddingTop: Int = 20
    protected var mPaddingBottom: Int = 20
    protected var mMinHeightOfContent: Int = 0


    init {
        mSpinnerStyle = SpinnerStyle.Translate
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val thisView: View = this
        if (mMinHeightOfContent == 0) {
            mPaddingTop = thisView.paddingTop
            mPaddingBottom = thisView.paddingBottom
            if (mPaddingTop == 0 || mPaddingBottom == 0) {
                val paddingLeft = thisView.getPaddingLeft()
                val paddingRight = thisView.getPaddingRight()
                mPaddingTop = if (mPaddingTop == 0) SmartUtil.dp2px(20f) else mPaddingTop
                mPaddingBottom = if (mPaddingBottom == 0) SmartUtil.dp2px(20f) else mPaddingBottom
                thisView.setPadding(paddingLeft, mPaddingTop, paddingRight, mPaddingBottom)
            }
            val thisGroup: ViewGroup = this
            thisGroup.clipToPadding = false
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
            if (parentHeight < mMinHeightOfContent) {
                val padding = (parentHeight - mMinHeightOfContent) / 2
                thisView.setPadding(
                    thisView.getPaddingLeft(),
                    padding,
                    thisView.getPaddingRight(),
                    padding
                )
            } else {
                thisView.setPadding(thisView.getPaddingLeft(), 0, thisView.getPaddingRight(), 0)
            }
        } else {
            thisView.setPadding(
                thisView.getPaddingLeft(),
                mPaddingTop,
                thisView.getPaddingRight(),
                mPaddingBottom
            )
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mMinHeightOfContent == 0) {
            val thisGroup: ViewGroup = this
            for (i in 0..<thisGroup.size) {
                val height = thisGroup.getChildAt(i).measuredHeight
                if (mMinHeightOfContent < height) {
                    mMinHeightOfContent = height
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val arrowView: View? = mArrowView
        val progressView: View? = mProgressView
        arrowView?.animate()?.cancel()
        progressView?.animate()?.cancel()
        val drawable = mProgressView?.getDrawable()
        if (drawable is Animatable) {
            if ((drawable as Animatable).isRunning) {
                (drawable as Animatable).stop()
            }
        }
    }


    @Suppress("UNCHECKED_CAST")
    protected fun self(): T? {
        return this as T
    }


    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
        mRefreshKernel = kernel
        mRefreshKernel?.requestDrawBackgroundFor(this, mBackgroundColor)
    }

    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        val progressView: View? = mProgressView
        if (progressView?.visibility != VISIBLE) {
            progressView?.visibility = VISIBLE
            val drawable = mProgressView?.getDrawable()
            if (drawable is Animatable) {
                (drawable as Animatable).start()
            } else {
                progressView?.animate()?.rotation(36000f)?.setDuration(100000)
            }
        }
    }

    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        onStartAnimator(refreshLayout, height, maxDragHeight)
    }

    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
        val progressView: View? = mProgressView
        val drawable = mProgressView?.getDrawable()
        if (drawable is Animatable) {
            if ((drawable as Animatable).isRunning) {
                (drawable as Animatable).stop()
            }
        } else {
            progressView?.animate()?.rotation(0f)?.setDuration(0)
        }
        progressView?.visibility = GONE
        //延迟500毫秒之后再弹回
        return mFinishDuration
    }

    override fun setPrimaryColors(@ColorInt vararg colors: Int) {
        if (colors.isNotEmpty()) {
            val thisView: View = this
            if (thisView.background !is BitmapDrawable && !mSetPrimaryColor) {
                setPrimaryColor(colors[0])
                mSetPrimaryColor = false
            }
            if (!mSetAccentColor) {
                if (colors.size > 1) {
                    setAccentColor(colors[1])
                }
                mSetAccentColor = false
            }
        }
    }


    fun setProgressBitmap(bitmap: Bitmap?): T? {
        mProgressDrawable = null
        mProgressView?.setImageBitmap(bitmap)
        return self()
    }

    fun setProgressDrawable(drawable: Drawable?): T? {
        mProgressDrawable = null
        mProgressView?.setImageDrawable(drawable)
        return self()
    }

    fun setProgressResource(@DrawableRes resId: Int): T? {
        mProgressDrawable = null
        mProgressView?.setImageResource(resId)
        return self()
    }

    fun setArrowBitmap(bitmap: Bitmap?): T? {
        mArrowDrawable = null
        mArrowView?.setImageBitmap(bitmap)
        return self()
    }

    fun setArrowDrawable(drawable: Drawable?): T? {
        mArrowDrawable = null
        mArrowView?.setImageDrawable(drawable)
        return self()
    }

    fun setArrowResource(@DrawableRes resId: Int): T? {
        mArrowDrawable = null
        mArrowView?.setImageResource(resId)
        return self()
    }

    fun setSpinnerStyle(style: SpinnerStyle?): T? {
        this.mSpinnerStyle = style
        return self()
    }

    fun setPrimaryColor(@ColorInt primaryColor: Int): T? {
        mSetPrimaryColor = true
        mBackgroundColor = primaryColor
        mRefreshKernel?.requestDrawBackgroundFor(this, primaryColor)
        return self()
    }

    open fun setAccentColor(@ColorInt accentColor: Int): T? {
        mSetAccentColor = true
        mTitleText?.setTextColor(accentColor)
        mArrowDrawable?.apply {
            setColor(accentColor)
            mArrowView?.invalidateDrawable(this)
        }
        mProgressDrawable?.apply {
            setColor(accentColor)
            mProgressView?.invalidateDrawable(this)
        }
        return self()
    }

    fun setPrimaryColorId(@ColorRes colorId: Int): T? {
        val thisView: View = this
        setPrimaryColor(ContextCompat.getColor(thisView.context, colorId))
        return self()
    }

    fun setAccentColorId(@ColorRes colorId: Int): T? {
        val thisView: View = this
        setAccentColor(ContextCompat.getColor(thisView.context, colorId))
        return self()
    }

    fun setFinishDuration(delay: Int): T? {
        mFinishDuration = delay
        return self()
    }

    fun setTextSizeTitle(size: Float): T? {
        mTitleText?.textSize = size
        mRefreshKernel?.requestRemeasureHeightFor(this)
        return self()
    }

    fun setTextSizeTitle(unit: Int, size: Float): T? {
        mTitleText?.setTextSize(unit, size)
        mRefreshKernel?.requestRemeasureHeightFor(this)
        return self()
    }

    fun setDrawableMarginRight(dp: Float): T? {
        val arrowView: View? = mArrowView
        val progressView: View? = mProgressView
        val lpArrow = arrowView?.layoutParams as MarginLayoutParams
        val lpProgress = progressView?.layoutParams as MarginLayoutParams
        lpProgress.rightMargin = SmartUtil.dp2px(dp)
        lpArrow.rightMargin = lpProgress.rightMargin
        arrowView.setLayoutParams(lpArrow)
        progressView.setLayoutParams(lpProgress)
        return self()
    }

    fun setDrawableMarginRightPx(px: Int): T? {
        val lpArrow = mArrowView?.layoutParams as MarginLayoutParams
        val lpProgress = mProgressView?.layoutParams as MarginLayoutParams
        lpProgress.rightMargin = px
        lpArrow.rightMargin = lpProgress.rightMargin
        mArrowView?.setLayoutParams(lpArrow)
        mProgressView?.setLayoutParams(lpProgress)
        return self()
    }

    fun setDrawableSize(dp: Float): T? {
        val arrowView: View? = mArrowView
        val progressView: View? = mProgressView
        val lpArrow = arrowView?.layoutParams
        val lpProgress = progressView?.layoutParams
        val dp2Px = SmartUtil.dp2px(dp)
        lpProgress?.width = dp2Px
        lpArrow?.width = dp2Px
        lpProgress?.height = dp2Px
        lpArrow?.height = dp2Px
        arrowView?.setLayoutParams(lpArrow)
        progressView?.setLayoutParams(lpProgress)
        return self()
    }

    fun setDrawableSizePx(px: Int): T? {
        val lpArrow = mArrowView?.layoutParams
        val lpProgress = mProgressView?.layoutParams
        lpProgress?.width = px
        lpProgress?.height = px
        lpArrow?.width = px
        lpArrow?.height = px
        mArrowView?.setLayoutParams(lpArrow)
        mProgressView?.setLayoutParams(lpProgress)
        return self()
    }

    fun setDrawableArrowSize(dp: Float): T? {
        val arrowView: View? = mArrowView
        val lpArrow = arrowView?.layoutParams
        lpArrow?.width = SmartUtil.dp2px(dp)
        lpArrow?.height = lpArrow.width
        arrowView?.setLayoutParams(lpArrow)
        return self()
    }

    fun setDrawableArrowSizePx(px: Int): T? {
        val lpArrow = mArrowView?.layoutParams
        lpArrow?.width = px
        lpArrow?.height = px
        mArrowView?.setLayoutParams(lpArrow)
        return self()
    }

    fun setDrawableProgressSize(dp: Float): T? {
        val progressView: View? = mProgressView
        val lpProgress = progressView?.layoutParams
        lpProgress?.width = SmartUtil.dp2px(dp)
        lpProgress?.height = lpProgress.width
        progressView?.setLayoutParams(lpProgress)
        return self()
    }

    fun setDrawableProgressSizePx(px: Int): T? {
        val lpProgress = mProgressView?.layoutParams
        lpProgress?.width = px
        lpProgress?.height = px
        mProgressView?.setLayoutParams(lpProgress)
        return self()
    }

    companion object {
        val ID_TEXT_TITLE: Int = R.id.srl_classics_title
        val ID_IMAGE_ARROW: Int = R.id.srl_classics_arrow
        val ID_IMAGE_PROGRESS: Int = R.id.srl_classics_progress
    }
}
