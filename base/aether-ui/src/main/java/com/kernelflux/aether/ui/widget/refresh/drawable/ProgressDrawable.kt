package com.kernelflux.aether.ui.widget.refresh.drawable

import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import kotlin.math.max
import androidx.core.graphics.withRotation

/**
 * 旋转动画
 * Created by scwang on 2017/6/16.
 */
open class ProgressDrawable : PaintDrawable(), Animatable, AnimatorUpdateListener {
    protected var mWidth: Int = 0
    protected var mHeight: Int = 0
    protected var mProgressDegree: Int = 0
    protected var mValueAnimator: ValueAnimator = ValueAnimator.ofInt(30, 3600)
    protected var mPath: Path = Path()

    init {
        mValueAnimator.setDuration(10000)
        mValueAnimator.interpolator = null
        mValueAnimator.repeatCount = ValueAnimator.INFINITE
        mValueAnimator.repeatMode = ValueAnimator.RESTART
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val value = animation.getAnimatedValue() as Int
        mProgressDegree = 30 * (value / 30)
        val drawable: Drawable = this@ProgressDrawable
        drawable.invalidateSelf()
    }


    override fun draw(canvas: Canvas) {
        val drawable: Drawable = this@ProgressDrawable
        val bounds = drawable.getBounds()
        val width = bounds.width()
        val height = bounds.height()
        val r = max(1f, width / 22f)

        if (mWidth != width || mHeight != height) {
            mPath.reset()
            mPath.addCircle(width - r, height / 2f, r, Path.Direction.CW)
            mPath.addRect(
                width - 5 * r,
                height / 2f - r,
                width - r,
                height / 2f + r,
                Path.Direction.CW
            )
            mPath.addCircle(width - 5 * r, height / 2f, r, Path.Direction.CW)
            mWidth = width
            mHeight = height
        }

        canvas.withRotation(mProgressDegree.toFloat(), (width) / 2f, (height) / 2f) {
            for (i in 0..11) {
                mPaint.setAlpha((i + 5) * 0x11)
                rotate(30f, (width) / 2f, (height) / 2f)
                drawPath(mPath, mPaint)
            }
        }
    }

    override fun start() {
        if (!mValueAnimator.isRunning) {
            mValueAnimator.addUpdateListener(this)
            mValueAnimator.start()
        }
    }

    override fun stop() {
        if (mValueAnimator.isRunning) {
            val animator: Animator = mValueAnimator
            animator.removeAllListeners()
            mValueAnimator.removeAllUpdateListeners()
            mValueAnimator.cancel()
        }
    }

    override fun isRunning(): Boolean {
        return mValueAnimator.isRunning
    }
}
