package com.kernelflux.aether.ui.widget.safewidget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView


open class SafeRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    private val mSafeRecyclerViewHelper: SafeRecyclerViewHelper = SafeRecyclerViewHelper(this)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mSafeRecyclerViewHelper.onAttachedToWindow(this)
    }

    override fun onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow()
            mSafeRecyclerViewHelper.onDetachedFromWindow()
        } catch (e: Throwable) {
            //
        }
    }
}