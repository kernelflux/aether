package com.kernelflux.aether.common

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.DimenRes
import androidx.fragment.app.Fragment

val Float.dp: Int
    get() = toPx(this, isSp = false)

val Float.sp: Int
    get() = toPx(this, isSp = true)

val Int.dp: Int
    get() = toPx(this.toFloat(), isSp = false)

val Int.sp: Int
    get() = toPx(this.toFloat(), isSp = true)

private fun toPx(value: Float, isSp: Boolean): Int {
    val context = UtilsConfig.getAppContext() ?: return 0
    val metrics = context.resources.displayMetrics
    val unit = if (isSp) TypedValue.COMPLEX_UNIT_SP else TypedValue.COMPLEX_UNIT_DIP
    return TypedValue.applyDimension(unit, value, metrics).toInt()
}

fun getPixelSizeInt(@DimenRes resId: Int, context: Context? = null): Int {
    val cxt = context ?: UtilsConfig.getAppContext()
    return cxt?.resources?.getDimensionPixelSize(resId) ?: 0
}

fun getPixelSizeFloat(@DimenRes resId: Int, context: Context? = null): Float {
    val cxt = context ?: UtilsConfig.getAppContext()
    return cxt?.resources?.getDimension(resId) ?: 0f
}

fun View.pxInt(@DimenRes resId: Int): Int = getPixelSizeInt(resId, context)
fun Fragment.pxInt(@DimenRes resId: Int): Int = getPixelSizeInt(resId, context)

fun View.pxFloat(@DimenRes resId: Int): Float = getPixelSizeFloat(resId, context)
fun Fragment.pxFloat(@DimenRes resId: Int): Float = getPixelSizeFloat(resId, context)
