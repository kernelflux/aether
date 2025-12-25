package com.kernelflux.aether.ui.widget.refresh.drawable

import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * 画笔 Drawable
 * Created by scwang on 2017/6/16.
 */
abstract class PaintDrawable protected constructor() : Drawable() {
    @JvmField
    protected var mPaint: Paint = Paint()

    init {
        mPaint.style = Paint.Style.FILL
        mPaint.isAntiAlias = true
        mPaint.setColor(-0x555556)
    }

    fun setColor(color: Int) {
        mPaint.setColor(color)
    }

    override fun setAlpha(alpha: Int) {
        mPaint.setAlpha(alpha)
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint.setColorFilter(cf)
    }

    @Deprecated("")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
