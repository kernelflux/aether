package com.kernelflux.aether.ui.widget.refresh.util

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.PointF
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.webkit.WebView
import android.widget.AbsListView
import android.widget.ListView
import android.widget.ScrollView
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingParent
import androidx.core.view.ScrollingView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.kernelflux.aether.ui.widget.refresh.api.RefreshComponent
import kotlin.math.exp
import androidx.core.view.isVisible
import androidx.core.view.size
import com.kernelflux.aether.ui.R

/**
 * SmartUtil
 * Created by scwang on 2018/3/5.
 */
class SmartUtil(private val type: Int) : Interpolator {
    override fun getInterpolation(input: Float): Float {
        if (type == INTERPOLATOR_DECELERATE) {
            return (1.0f - (1.0f - input) * (1.0f - input))
        }
        val interpolated: Float = VISCOUS_FLUID_NORMALIZE * viscousFluid(input)
        if (interpolated > 0) {
            return interpolated + VISCOUS_FLUID_OFFSET
        }
        return interpolated
    }

    companion object {
        @JvmField
        var INTERPOLATOR_VISCOUS_FLUID: Int = 0
        var INTERPOLATOR_DECELERATE: Int = 1

        @JvmStatic
        fun measureViewHeight(view: View): Int {
            var p = view.layoutParams
            if (p == null) {
                p = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width)
            val childHeightSpec: Int = if (p.height > 0) {
                View.MeasureSpec.makeMeasureSpec(p.height, View.MeasureSpec.EXACTLY)
            } else {
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            }
            view.measure(childWidthSpec, childHeightSpec)
            return view.measuredHeight
        }

        @JvmStatic
        @SuppressLint("ObsoleteSdkInt")
        fun scrollListBy(listView: AbsListView, y: Int) {
            if (Build.VERSION.SDK_INT >= 19) {
                // Call the framework version directly
                listView.scrollListBy(y)
            } else if (listView is ListView) {
                // provide backport on earlier versions
                val firstPosition = listView.firstVisiblePosition
                if (firstPosition == ListView.INVALID_POSITION) {
                    return
                }

                val listGroup: ViewGroup = listView
                val firstView = listGroup.getChildAt(0)
                if (firstView == null) {
                    return
                }

                val newTop = firstView.top - y
                listView.setSelectionFromTop(firstPosition, newTop)
            } else {
                listView.smoothScrollBy(y, 0)
            }
        }

        fun isScrollableView(view: View?): Boolean {
            if (view is RefreshComponent) {
                return false
            }
            return view is AbsListView
                    || view is ScrollView
                    || view is ScrollingView
                    || view is WebView
                    || view is NestedScrollingChild
        }

        @JvmStatic
        fun isContentView(view: View?): Boolean {
            if (view is RefreshComponent) {
                return false
            }
            return isScrollableView(view)
                    || view is ViewPager
                    || view is ViewPager2
                    || view is NestedScrollingParent
        }

        @JvmStatic
        fun fling(scrollableView: View?, velocity: Int) {
            when (scrollableView) {
                is ScrollView -> {
                    scrollableView.fling(velocity)
                }

                is AbsListView -> {
                    scrollableView.fling(velocity)
                }

                is WebView -> {
                    scrollableView.flingScroll(0, velocity)
                }

                is NestedScrollView -> {
                    scrollableView.fling(velocity)
                }

                is RecyclerView -> {
                    scrollableView.fling(0, velocity)
                }
            }
        }

        /**
         * 判断内容是否可以刷新
         * @param targetView 内容视图
         * @param touch 按压事件位置
         * @return 是否可以刷新
         */
        fun canRefresh(targetView: View, touch: PointF?): Boolean {
            if (targetView.canScrollVertically(-1) && targetView.isVisible) {
                return false
            }
            //touch == null 时 canRefresh 不会动态递归搜索
            if (targetView is ViewGroup && touch != null) {
                val viewGroup = targetView
                val childCount = viewGroup.size
                val point = PointF()
                for (i in childCount downTo 1) {
                    val child = viewGroup.getChildAt(i - 1)
                    if (isTransformedTouchPointInView(viewGroup, child, touch.x, touch.y, point)) {
                        val tag = child.getTag(R.id.srl_tag)
                        if ("fixed" == tag || "fixed-bottom" == tag) {
                            return false
                        }
                        touch.offset(point.x, point.y)
                        val can: Boolean = canRefresh(child, touch)
                        touch.offset(-point.x, -point.y)
                        return can
                    }
                }
            }
            return true
        }

        /**
         * 判断内容视图是否可以加载更多
         * @param targetView 内容视图
         * @param touch 按压事件位置
         * @param contentFull 内容是否填满页面 (未填满时，会通过canScrollUp自动判断)
         * @return 是否可以刷新
         */
        fun canLoadMore(targetView: View, touch: PointF?, contentFull: Boolean): Boolean {
            if (targetView.canScrollVertically(1) && targetView.isVisible) {
                return false
            }
            //touch == null 时 canLoadMore 不会动态递归搜索
            if (targetView is ViewGroup && touch != null && !isScrollableView(targetView)) {
                val viewGroup = targetView
                val childCount = viewGroup.size
                val point = PointF()
                for (i in childCount downTo 1) {
                    val child = viewGroup.getChildAt(i - 1)
                    if (isTransformedTouchPointInView(viewGroup, child, touch.x, touch.y, point)) {
                        val tag = child.getTag(R.id.srl_tag)
                        if ("fixed" == tag || "fixed-top" == tag) {
                            return false
                        }
                        touch.offset(point.x, point.y)
                        val can: Boolean = canLoadMore(child, touch, contentFull)
                        touch.offset(-point.x, -point.y)
                        return can
                    }
                }
            }
            return (contentFull || targetView.canScrollVertically(-1))
        }

        @JvmStatic
        fun isTransformedTouchPointInView(
            group: View,
            child: View,
            x: Float,
            y: Float,
            outLocalPoint: PointF?
        ): Boolean {
            if (child.visibility != View.VISIBLE) {
                return false
            }
            val point = FloatArray(2)
            point[0] = x
            point[1] = y
            //        transformPointToViewLocal(group, child, point);
            point[0] += (group.scrollX - child.left).toFloat()
            point[1] += (group.scrollY - child.top).toFloat()
            //        final boolean isInView = pointInView(child, point[0], point[1], 0);
            val isInView =
                point[0] >= 0 && point[1] >= 0 && point[0] < (child.width) && point[1] < ((child.height))
            if (isInView && outLocalPoint != null) {
                outLocalPoint.set(point[0] - x, point[1] - y)
            }
            return isInView
        }

        private val density = Resources.getSystem().displayMetrics.density

        /**
         * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
         * @param dpValue 虚拟像素
         * @return 像素
         */
        @JvmStatic
        fun dp2px(dpValue: Float): Int {
            return (0.5f + dpValue * density).toInt()
        }

        /**
         * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
         * @param pxValue 像素
         * @return 虚拟像素
         */
        @JvmStatic
        fun px2dp(pxValue: Int): Float {
            return (pxValue / density)
        }

        /** Controls the viscous fluid effect (how much of it).  */
        private const val VISCOUS_FLUID_SCALE = 8.0f

        private val VISCOUS_FLUID_NORMALIZE: Float
        private val VISCOUS_FLUID_OFFSET: Float

        init {
            // must be set to 1.0 (used in viscousFluid())
            VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f)
            // account for very small floating-point error
            VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f)
        }

        private fun viscousFluid(x: Float): Float {
            var x = x
            x *= VISCOUS_FLUID_SCALE
            if (x < 1.0f) {
                x -= (1.0f - exp(-x.toDouble()).toFloat())
            } else {
                val start = 0.36787945f // 1/e == exp(-1)
                x = 1.0f - exp((1.0f - x).toDouble()).toFloat()
                x = start + x * (1.0f - start)
            }
            return x
        }
    }
}
