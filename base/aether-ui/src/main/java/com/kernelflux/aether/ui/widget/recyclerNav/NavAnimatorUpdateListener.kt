package com.kernelflux.aether.ui.widget.recyclerNav

import android.animation.ValueAnimator
import android.view.View
import java.lang.ref.WeakReference


class NavAnimatorUpdateListener(
    recyclerNav: RecyclerNav?,
    view: View?,
    focusItemOffsetListener: RecyclerNav.NavFocusItemOffsetListener?,
    position: Int,
    navAnimationProgressListener: RecyclerNav.NavAnimationProgressListener?
) : ValueAnimator.AnimatorUpdateListener {
    private var mRecyclerNavWeakRef: WeakReference<RecyclerNav>? = null
    private var mViewWeakRef: WeakReference<View>? = null
    private var mNavAnimationStateListenerRef: WeakReference<RecyclerNav.NavAnimationProgressListener>? =
        null
    private var mNavFocusItemOffsetListener: RecyclerNav.NavFocusItemOffsetListener? = null
    private val position: Int

    init {
        mRecyclerNavWeakRef = WeakReference(recyclerNav)
        mViewWeakRef = WeakReference(view)
        mNavFocusItemOffsetListener = if (mNavFocusItemOffsetListener == null) {
            NavDefaultFocusItemOffsetListener(NavDefaultFocusItemOffsetListener.Companion.VERTICAL)
        } else {
            focusItemOffsetListener
        }
        this.position = position
        mNavAnimationStateListenerRef = WeakReference(navAnimationProgressListener)
    }

    private fun getNavAnimationProgressListener(): RecyclerNav.NavAnimationProgressListener? {
        return mNavAnimationStateListenerRef?.get()
    }

    private fun getRecyclerViewNav(): RecyclerNav? {
        return mRecyclerNavWeakRef?.get()
    }

    private fun getView(): View? {
        return mViewWeakRef?.get()
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val animatedFraction = animation.animatedFraction
        val recyclerNav = getRecyclerViewNav()
        val view = getView()
        val navAnimationProgressListener = getNavAnimationProgressListener()
        if (!(recyclerNav == null || view == null)) {
            for (index in 0..recyclerNav.childCount) {
                (recyclerNav.getChildViewHolder(recyclerNav.getChildAt(index)) as? NavViewHolder)?.onAnimationUpdate(
                    animatedFraction,
                    view,
                    position,
                    recyclerNav
                )
            }
            recyclerNav.scrollBy(
                calculateScrollXDistance(
                    animatedFraction,
                    0,
                    view.left - (mNavFocusItemOffsetListener?.getFocusItemOffset(view, recyclerNav)
                        ?: 0)
                ),
                0
            )
        }
        navAnimationProgressListener?.onProgress(
            position,
            animatedFraction
        )
    }

    private fun calculateScrollXDistance(
        fraction: Float,
        startValue: Int,
        endValue: Int
    ): Int {
        return startValue + (endValue - startValue) * fraction.toInt()
    }
}