package com.kernelflux.aether.ui.widget.refresh.classics

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Drawable
import com.kernelflux.aether.ui.widget.refresh.drawable.PaintDrawable

/**
 * 箭头图像
 * Created by scwang on 2018/2/5.
 */
class ArrowDrawable : PaintDrawable() {
    private var mWidth = 0
    private var mHeight = 0
    private val mPath = Path()

    override fun draw(canvas: Canvas) {
        val drawable: Drawable = this@ArrowDrawable
        val bounds = drawable.getBounds()
        val width = bounds.width()
        val height = bounds.height()
        if (mWidth != width || mHeight != height) {
            val lineWidth = width * 30 / 225
            mPath.reset()

            val vector1 = (lineWidth * 0.70710677f) //Math.sin(Math.PI/4));
            val vector2 = (lineWidth / 0.70710677f) //Math.sin(Math.PI/4));
            mPath.moveTo(width / 2f, height.toFloat())
            mPath.lineTo(0f, height / 2f)
            mPath.lineTo(vector1, height / 2f - vector1)
            mPath.lineTo(width / 2f - lineWidth / 2f, height - vector2 - lineWidth / 2f)
            mPath.lineTo(width / 2f - lineWidth / 2f, 0f)
            mPath.lineTo(width / 2f + lineWidth / 2f, 0f)
            mPath.lineTo(width / 2f + lineWidth / 2f, height - vector2 - lineWidth / 2f)
            mPath.lineTo(width - vector1, height / 2f - vector1)
            mPath.lineTo(width.toFloat(), height / 2f)
            mPath.close()

            mWidth = width
            mHeight = height
        }
        canvas.drawPath(mPath, mPaint)
    }
}
