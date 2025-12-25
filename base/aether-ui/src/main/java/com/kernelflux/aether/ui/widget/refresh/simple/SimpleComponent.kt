package com.kernelflux.aether.ui.widget.refresh.simple

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import com.kernelflux.aether.ui.widget.refresh.SmartRefreshLayout
import com.kernelflux.aether.ui.widget.refresh.api.RefreshComponent
import com.kernelflux.aether.ui.widget.refresh.api.RefreshFooter
import com.kernelflux.aether.ui.widget.refresh.api.RefreshHeader
import com.kernelflux.aether.ui.widget.refresh.api.RefreshKernel
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout
import com.kernelflux.aether.ui.widget.refresh.constant.RefreshState
import com.kernelflux.aether.ui.widget.refresh.constant.SpinnerStyle
import com.kernelflux.aether.ui.widget.refresh.listener.OnStateChangedListener

/**
 * Component 初步实现
 * 实现 Header 和 Footer 时，继承 SimpleComponent 的话可以少写很多接口方法
 * Created by scwang on 2018/2/6.
 */
abstract class SimpleComponent : RelativeLayout, RefreshComponent {
    protected var mWrappedView: View? = null
    protected var mSpinnerStyle: SpinnerStyle? = null
    protected var mWrappedInternal: RefreshComponent? = null

    protected constructor(wrapped: View) : this(
        wrapped,
        if (wrapped is RefreshComponent) wrapped as RefreshComponent else null
    )

    protected constructor(
        wrappedView: View,
        wrappedInternal: RefreshComponent?
    ) : super(wrappedView.context, null, 0) {
        this.mWrappedView = wrappedView
        this.mWrappedInternal = wrappedInternal
        if (this is RefreshFooter &&
            mWrappedInternal is RefreshHeader &&
            mWrappedInternal?.getSpinnerStyle() === SpinnerStyle.MatchLayout
        ) {
            wrappedInternal?.getView()?.scaleY = -1f
        } else if (this is RefreshHeader &&
            mWrappedInternal is RefreshFooter &&
            mWrappedInternal?.getSpinnerStyle() === SpinnerStyle.MatchLayout
        ) {
            wrappedInternal?.getView()?.scaleY = -1f
        }
    }

    protected constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) {
            if (other is RefreshComponent) {
                val thisView: RefreshComponent = this
                return thisView.getView() === other.getView()
            }
            return false
        }
        return true
    }

    override fun getView(): View {
        return mWrappedView ?: this
    }

    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            return mWrappedInternal?.onFinish(refreshLayout, success) ?: 0
        }
        return 0
    }

    override fun setPrimaryColors(@ColorInt vararg colors: Int) {
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            mWrappedInternal?.setPrimaryColors(*colors)
        }
    }

    override fun getSpinnerStyle(): SpinnerStyle {
        if (mSpinnerStyle != null) {
            return mSpinnerStyle!!
        }
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            return mWrappedInternal!!.getSpinnerStyle()
        }
        if (mWrappedView != null) {
            val params = mWrappedView!!.layoutParams
            if (params is SmartRefreshLayout.LayoutParams) {
                mSpinnerStyle = params.spinnerStyle
                if (mSpinnerStyle != null) {
                    return mSpinnerStyle!!
                }
            }
            if (params != null) {
                if (params.height == 0 || params.height == LayoutParams.MATCH_PARENT) {
                    for (style in SpinnerStyle.values) {
                        if (style.scale) {
                            return style.also { mSpinnerStyle = it }
                        }
                    }
                }
            }
        }
        return SpinnerStyle.Translate.also { mSpinnerStyle = it }
    }

    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            mWrappedInternal?.onInitialized(kernel, height, maxDragHeight)
        } else if (mWrappedView != null) {
            val params = mWrappedView?.layoutParams
            if (params is SmartRefreshLayout.LayoutParams) {
                kernel.requestDrawBackgroundFor(this, params.backgroundColor)
            }
        }
    }

    override fun isSupportHorizontalDrag(): Boolean {
        return mWrappedInternal != null && mWrappedInternal !== this && mWrappedInternal?.isSupportHorizontalDrag() == true
    }

    override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            mWrappedInternal?.onHorizontalDrag(percentX, offsetX, offsetMax)
        }
    }

    override fun onMoving(
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        height: Int,
        maxDragHeight: Int
    ) {
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            mWrappedInternal?.onMoving(isDragging, percent, offset, height, maxDragHeight)
        }
    }

    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            mWrappedInternal?.onReleased(refreshLayout, height, maxDragHeight)
        }
    }

    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            mWrappedInternal?.onStartAnimator(refreshLayout, height, maxDragHeight)
        }
    }

    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState
    ) {
        var oldState = oldState
        var newState = newState
        if (mWrappedInternal != null && mWrappedInternal !== this) {
            if (this is RefreshFooter && mWrappedInternal is RefreshHeader) {
                if (oldState.isFooter) {
                    oldState = oldState.toHeader()
                }
                if (newState.isFooter) {
                    newState = newState.toHeader()
                }
            } else if (this is RefreshHeader && mWrappedInternal is RefreshFooter) {
                if (oldState.isHeader) {
                    oldState = oldState.toFooter()
                }
                if (newState.isHeader) {
                    newState = newState.toFooter()
                }
            }
            val listener: OnStateChangedListener? = mWrappedInternal
            listener?.onStateChanged(refreshLayout, oldState, newState)
        }
    }

    @SuppressLint("RestrictedApi")
    open fun setNoMoreData(noMoreData: Boolean): Boolean {
        return mWrappedInternal is RefreshFooter && (mWrappedInternal as RefreshFooter).setNoMoreData(
            noMoreData
        )
    }

    override fun autoOpen(duration: Int, dragRate: Float, animationOnly: Boolean): Boolean {
        return false
    }

    override fun hashCode(): Int {
        var result = mWrappedView?.hashCode() ?: 0
        result = 31 * result + (mSpinnerStyle?.hashCode() ?: 0)
        result = 31 * result + (mWrappedInternal?.hashCode() ?: 0)
        return result
    }
}
