package com.kernelflux.aether.ui.widget.refresh.wrapper

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.PointF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.Space
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingParent
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.kernelflux.aether.ui.widget.refresh.api.RefreshContent
import com.kernelflux.aether.ui.widget.refresh.api.RefreshKernel
import com.kernelflux.aether.ui.widget.refresh.listener.CoordinatorLayoutListener
import com.kernelflux.aether.ui.widget.refresh.listener.ScrollBoundaryDecider
import com.kernelflux.aether.ui.widget.refresh.simple.SimpleBoundaryDecider
import com.kernelflux.aether.ui.widget.refresh.util.DesignUtil.checkCoordinatorLayout
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil.Companion.isContentView
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil.Companion.isTransformedTouchPointInView
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil.Companion.measureViewHeight
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil.Companion.scrollListBy
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min
import androidx.core.view.size
import com.kernelflux.aether.ui.R

/**
 * 刷新内容包装
 * Created by scwang on 2017/5/26.
 */
open class RefreshContentWrapper(view: View) : RefreshContent, CoordinatorLayoutListener,
    AnimatorUpdateListener {
    protected var mContentView: View //直接内容视图
    protected var mOriginalContentView: View //被包裹的原真实视图
    protected var mScrollableView: View
    protected var mFixedHeader: View? = null
    protected var mFixedFooter: View? = null
    protected var mLastSpinner: Int = 0
    protected var mEnableRefresh: Boolean = true
    protected var mEnableLoadMore: Boolean = true
    protected var mBoundaryAdapter: SimpleBoundaryDecider = SimpleBoundaryDecider()

    init {
        mScrollableView = view
        mOriginalContentView = view
        this.mContentView = mOriginalContentView
    }

    protected fun findScrollableView(content: View, kernel: RefreshKernel) {
        var content = content
        var scrollableView: View? = null
        val isInEditMode = mContentView.isInEditMode
        while (scrollableView == null || (scrollableView is NestedScrollingParent && scrollableView !is NestedScrollingChild)) {
            content = findScrollableViewInternal(content, scrollableView == null)
            if (content === scrollableView) {
                break
            }
            if (!isInEditMode) {
                checkCoordinatorLayout(content, kernel, this)
            }
            scrollableView = content
        }
        if (scrollableView != null) {
            mScrollableView = scrollableView
        }
    }

    override fun onCoordinatorUpdate(enableRefresh: Boolean, enableLoadMore: Boolean) {
        mEnableRefresh = enableRefresh
        mEnableLoadMore = enableLoadMore
    }

    protected fun findScrollableViewInternal(content: View, selfAble: Boolean): View {
        var scrollableView: View? = null
        val queue: LinkedList<View> = LinkedList()
        queue.add(content)

        while (queue.isNotEmpty() && scrollableView == null) {
            val view = queue.poll()
            if (view != null) {
                if ((selfAble || view !== content) && isContentView(view)) {
                    scrollableView = view
                } else if (view is ViewGroup) {
                    for (i in 0 until view.childCount) {
                        queue.add(view.getChildAt(i))
                    }
                }
            }
        }

        return scrollableView ?: content
    }

    protected fun findScrollableViewByPoint(
        content: View?,
        event: PointF?,
        orgScrollableView: View
    ): View {
        if (content is ViewGroup && event != null) {
            val viewGroup = content
            val childCount = viewGroup.size
            val point = PointF()
            for (i in childCount downTo 1) {
                var child = viewGroup.getChildAt(i - 1)
                if (isTransformedTouchPointInView(viewGroup, child, event.x, event.y, point)) {
                    if (child is ViewPager || child is ViewPager2 || !isContentView(child)) {
                        event.offset(point.x, point.y)
                        child = findScrollableViewByPoint(child, event, orgScrollableView)
                        event.offset(-point.x, -point.y)
                    }
                    return child
                }
            }
        }
        return orgScrollableView
    }


    override fun getView(): View {
        return mContentView
    }

    override fun getScrollableView(): View {
        return mScrollableView
    }

    override fun moveSpinner(
        spinner: Int,
        headerTranslationViewId: Int,
        footerTranslationViewId: Int
    ) {
        var translated = false
        if (headerTranslationViewId != View.NO_ID) {
            val headerTranslationView =
                mOriginalContentView.findViewById<View?>(headerTranslationViewId)
            if (headerTranslationView != null) {
                if (spinner > 0) {
                    translated = true
                    headerTranslationView.translationY = spinner.toFloat()
                } else if (headerTranslationView.translationY > 0) {
                    headerTranslationView.translationY = 0f
                }
            }
        }
        if (footerTranslationViewId != View.NO_ID) {
            val footerTranslationView =
                mOriginalContentView.findViewById<View>(footerTranslationViewId)
            if (footerTranslationView != null) {
                if (spinner < 0) {
                    translated = true
                    footerTranslationView.translationY = spinner.toFloat()
                } else if (footerTranslationView.translationY < 0) {
                    footerTranslationView.translationY = 0f
                }
            }
        }
        if (!translated) {
            mOriginalContentView.translationY = spinner.toFloat()
        } else {
            mOriginalContentView.translationY = 0f
        }
        mFixedHeader?.translationY = max(0, spinner).toFloat()
        mFixedFooter?.translationY = min(0, spinner).toFloat()
    }

    override fun canRefresh(): Boolean {
        return mEnableRefresh && mBoundaryAdapter.canRefresh(mContentView)
    }

    override fun canLoadMore(): Boolean {
        return mEnableLoadMore && mBoundaryAdapter.canLoadMore(mContentView)
    }

    override fun onActionDown(motionEvent: MotionEvent) {
        val point = PointF(motionEvent.getX(), motionEvent.getY())
        point.offset(-mContentView.left.toFloat(), -mContentView.top.toFloat())
        if (mScrollableView !== mContentView) {
            //如果内容视图不是 ScrollableView 说明使用了Layout嵌套内容，需要动态搜索 ScrollableView
            mScrollableView = findScrollableViewByPoint(
                mContentView,
                point,
                mScrollableView
            )
        }
        if (mScrollableView === mContentView) {
            //如果内容视图就是 ScrollableView 就不需要使用事件来动态搜索 而浪费CPU时间和性能了
            mBoundaryAdapter.mActionEvent = null
        } else {
            mBoundaryAdapter.mActionEvent = point
        }
    }

    override fun setUpComponent(kernel: RefreshKernel, fixedHeader: View?, fixedFooter: View?) {
        findScrollableView(mContentView, kernel)
        if (fixedHeader != null || fixedFooter != null) {
            mFixedHeader = fixedHeader
            mFixedFooter = fixedFooter
            val frameLayout: ViewGroup = FrameLayout(mContentView.context)
            var index: Int = kernel.getRefreshLayout().getLayout().indexOfChild(mContentView)
            kernel.getRefreshLayout().getLayout().removeView(mContentView)
            frameLayout.addView(
                mContentView,
                0,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            val layoutParams = mContentView.layoutParams
            kernel.getRefreshLayout().getLayout().addView(frameLayout, index, layoutParams)
            mContentView = frameLayout
            if (fixedHeader != null) {
                fixedHeader.setTag(R.id.srl_tag, "fixed-top")
                val lp = fixedHeader.layoutParams
                val parent = fixedHeader.parent as ViewGroup
                index = parent.indexOfChild(fixedHeader)
                parent.removeView(fixedHeader)
                lp.height = measureViewHeight(fixedHeader)
                parent.addView(Space(mContentView.context), index, lp)
                frameLayout.addView(fixedHeader, 1, lp)
            }
            if (fixedFooter != null) {
                fixedFooter.setTag(R.id.srl_tag, "fixed-bottom")
                val lp = fixedFooter.layoutParams
                val parent = fixedFooter.parent as ViewGroup
                index = parent.indexOfChild(fixedFooter)
                parent.removeView(fixedFooter)
                val flp = FrameLayout.LayoutParams(lp)
                lp.height = measureViewHeight(fixedFooter)
                parent.addView(Space(mContentView.context), index, lp)
                flp.gravity = Gravity.BOTTOM
                frameLayout.addView(fixedFooter, 1, flp)
            }
        }
    }

    override fun setScrollBoundaryDecider(boundary: ScrollBoundaryDecider) {
        if (boundary is SimpleBoundaryDecider) {
            mBoundaryAdapter = boundary
        } else {
            mBoundaryAdapter.boundary = (boundary)
        }
    }

    override fun setEnableLoadMoreWhenContentNotFull(enable: Boolean) {
        mBoundaryAdapter.mEnableLoadMoreWhenContentNotFull = enable
    }

    override fun scrollContentWhenFinished(spinner: Int): AnimatorUpdateListener? {
        return mScrollableView.let {
            if (spinner != 0) {
                if ((spinner < 0 && it.canScrollVertically(1)) ||
                    (spinner > 0 && it.canScrollVertically(-1))
                ) {
                    mLastSpinner = spinner
                    return this
                }
            }
            null
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val value = animation.getAnimatedValue() as Int
        mScrollableView.also {
            try {
                val dy = (value - mLastSpinner) * it.scaleY
                if (it is AbsListView) {
                    scrollListBy(mScrollableView as AbsListView, dy.toInt())
                } else {
                    it.scrollBy(0, dy.toInt())
                }
            } catch (e: Throwable) {
                //根据用户反馈，此处可能会有BUG
                e.printStackTrace()
            }
        }
        mLastSpinner = value
    }

}
